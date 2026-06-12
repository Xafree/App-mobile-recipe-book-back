package fr.gymgod.app.security.service;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import fr.gymgod.common.constants.SecurityConstants;
import fr.gymgod.common.exception.InvalidTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Date;

/**
 * Vérifie les identity tokens Apple (JWT) via les clés publiques JWKS d'Apple.
 *
 * <p>Flux : Flutter (sign_in_with_apple) → envoie {@code identity_token} →
 * le backend récupère les clés depuis {@code https://appleid.apple.com/auth/keys} →
 * vérifie la signature JWT et les claims.
 *
 * <p>Le JWKS est mis en cache en mémoire pendant {@value SecurityConstants#APPLE_JWKS_CACHE_TTL_MS} ms.
 */
@Service
@Slf4j
public class AppleTokenVerifier {

    @Value("${app.apple.app-id:}")
    private String expectedAppId;

    private String jwksUrl = SecurityConstants.APPLE_JWKS_URL;

    private volatile JWKSet cachedJwkSet;
    private volatile long   jwksCachedAtMs = 0L;

    public AppleTokenVerifier() {}

    AppleTokenVerifier(String expectedAppId, String jwksUrl) {
        this.expectedAppId = expectedAppId;
        this.jwksUrl = jwksUrl;
    }

    /**
     * Vérifie le token Apple et retourne les claims utilisateur.
     *
     * @param identityToken le token brut reçu de Flutter
     * @return les claims vérifiés (sub, email)
     * @throws InvalidTokenException si le token est invalide, expiré ou à signature incorrecte
     */
    public AppleClaims verify(String identityToken) {
        SignedJWT signedJWT = parseJwt(identityToken);
        JWKSet jwkSet = getJwkSetCached();
        verifySignature(signedJWT, jwkSet);
        return validateAndExtractClaims(signedJWT);
    }

    private SignedJWT parseJwt(String identityToken) {
        try {
            return SignedJWT.parse(identityToken);
        } catch (Exception exception) {
            throw new InvalidTokenException("Apple", "Impossible de parser l'identity token : " + exception.getMessage());
        }
    }

    private JWKSet getJwkSetCached() {
        long nowMs = Instant.now().toEpochMilli();
        if (cachedJwkSet != null && (nowMs - jwksCachedAtMs) < SecurityConstants.APPLE_JWKS_CACHE_TTL_MS) {
            return cachedJwkSet;
        }
        return refreshJwkSet(nowMs);
    }

    private synchronized JWKSet refreshJwkSet(long nowMs) {
        // Double-checked locking : un autre thread a peut-être rafraîchi entre-temps
        if (cachedJwkSet != null && (nowMs - jwksCachedAtMs) < SecurityConstants.APPLE_JWKS_CACHE_TTL_MS) {
            return cachedJwkSet;
        }
        try {
            String jwksJson = RestClient.create().get().uri(jwksUrl).retrieve().body(String.class);
            if (jwksJson == null || jwksJson.isBlank()) {
                throw new InvalidTokenException("Apple", "Réponse JWKS vide");
            }
            cachedJwkSet = JWKSet.parse(jwksJson);
            jwksCachedAtMs = Instant.now().toEpochMilli();
            log.debug("Apple JWKS rafraîchi avec succès");
            return cachedJwkSet;
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new InvalidTokenException("Apple", "Fetch JWKS échoué : " + exception.getMessage());
        } catch (Exception exception) {
            throw new InvalidTokenException("Apple", "Parse JWKS échoué : " + exception.getMessage());
        }
    }

    private void verifySignature(SignedJWT signedJWT, JWKSet jwkSet) {
        String keyId = signedJWT.getHeader().getKeyID();
        JWK matchingKey = jwkSet.getKeyByKeyId(keyId);
        if (matchingKey == null) {
            throw new InvalidTokenException("Apple", "Aucune clé trouvée dans le JWKS pour kid=" + keyId);
        }
        try {
            JWSVerifier verifier = new RSASSAVerifier(((RSAKey) matchingKey).toRSAPublicKey());
            if (!signedJWT.verify(verifier)) {
                throw new InvalidTokenException("Apple", "Vérification de signature JWT échouée");
            }
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidTokenException("Apple", "Erreur de vérification de signature : " + exception.getMessage());
        }
    }

    private AppleClaims validateAndExtractClaims(SignedJWT signedJWT) {
        try {
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            if (!SecurityConstants.APPLE_ISSUER.equals(claims.getIssuer())) {
                throw new InvalidTokenException("Apple", "Issuer invalide : " + claims.getIssuer());
            }
            if (!expectedAppId.isBlank() && !claims.getAudience().contains(expectedAppId)) {
                throw new InvalidTokenException("Apple", "Audience ne correspond pas à l'app ID");
            }

            Date expiration = claims.getExpirationTime();
            if (expiration == null || expiration.before(Date.from(Instant.now()))) {
                throw new InvalidTokenException("Apple", "Token expiré");
            }

            return new AppleClaims(claims.getSubject(), (String) claims.getClaim("email"));
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidTokenException("Apple", "Extraction des claims échouée : " + exception.getMessage());
        }
    }

    /** Claims extraits d'un token Apple vérifié. */
    public record AppleClaims(String sub, String email) {}
}

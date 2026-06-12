package fr.gymgod.app.security.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gymgod.common.constants.SecurityConstants;
import fr.gymgod.common.exception.InvalidTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Vérifie les ID tokens Google en appelant le endpoint tokeninfo de Google.
 *
 * <p>Flux : Flutter (google_sign_in) → envoie {@code id_token} →
 * le backend appelle {@code https://oauth2.googleapis.com/tokeninfo?id_token=...} →
 * valide l'audience → extrait les claims.
 */
@Service
@Slf4j
public class GoogleTokenVerifier {

    // Jackson 2.x statique — évite le conflit avec Jackson 3.x géré par Spring Boot 4.x
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${app.google.client-id:}")
    private String expectedClientId;

    private String tokenInfoBaseUrl = SecurityConstants.GOOGLE_TOKENINFO_URL;

    public GoogleTokenVerifier() {}

    GoogleTokenVerifier(String expectedClientId, String tokenInfoBaseUrl) {
        this.expectedClientId = expectedClientId;
        this.tokenInfoBaseUrl = tokenInfoBaseUrl;
    }

    /**
     * Vérifie le token Google et retourne les claims utilisateur.
     *
     * @param idToken le token brut reçu de Flutter
     * @return les claims vérifiés (sub, email, name)
     * @throws InvalidTokenException si le token est invalide, expiré ou d'une mauvaise audience
     */
    public GoogleClaims verify(String idToken) {
        String responseBody = callTokenInfoEndpoint(idToken);
        return parseClaims(responseBody);
    }

    private String callTokenInfoEndpoint(String idToken) {
        try {
            ResponseEntity<String> response = RestClient.create()
                    .get()
                    .uri(tokenInfoBaseUrl + idToken)
                    .retrieve()
                    .toEntity(String.class);
            return response.getBody();
        } catch (RestClientException exception) {
            log.warn("Google tokeninfo call failed: {}", exception.getMessage());
            throw new InvalidTokenException("Google", "Appel tokeninfo échoué");
        }
    }

    private GoogleClaims parseClaims(String responseBody) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(responseBody);

            if (node.has("error_description")) {
                throw new InvalidTokenException("Google", node.get("error_description").asText());
            }

            String audience = node.path("aud").asText();
            if (!expectedClientId.isBlank() && !expectedClientId.equals(audience)) {
                log.warn("Google token audience mismatch: expected={} got={}", expectedClientId, audience);
                throw new InvalidTokenException("Google", "Audience du token incorrecte");
            }

            return new GoogleClaims(
                    node.path("sub").asText(),
                    node.path("email").asText(),
                    node.path("name").asText(null),
                    node.path("picture").asText(null)
            );
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Failed to parse Google tokeninfo response: {}", exception.getMessage());
            throw new InvalidTokenException("Google", "Impossible de parser la réponse tokeninfo");
        }
    }

    /** Claims extraits d'un token Google vérifié. {@code picture} est l'URL
     * de la photo de profil Google (peut être null). */
    public record GoogleClaims(String sub, String email, String name, String picture) {}
}

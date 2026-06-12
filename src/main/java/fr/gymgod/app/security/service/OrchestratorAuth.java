package fr.gymgod.app.security.service;

import fr.gymgod.app.security.controller.dto.AppleAuthRequest;
import fr.gymgod.app.security.controller.dto.GoogleAuthRequest;
import fr.gymgod.app.security.domain.port.EmailPort;
import fr.gymgod.app.security.domain.port.SecurityDataPort;
import fr.gymgod.common.entities.user.AuthProvider;
import fr.gymgod.common.entities.user.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrchestratorAuth {

    private final SecurityDataPort securityDataPort;
    private final PasswordEncoder passwordEncoder;
    private final EmailPort emailPort;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final AppleTokenVerifier appleTokenVerifier;

    @Transactional
    public void registerUser(String username, String email, String password) {
        if (securityDataPort.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (securityDataPort.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setActive(true);
        user.setEmailVerified(false);

        String code = generateVerificationCode();
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));

        securityDataPort.save(user);

        try {
            emailPort.sendVerificationEmail(email, code);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email. Please check your email address.");
        }
    }

    public void verifyUser(String email, String code) {
        UserAccount user = securityDataPort.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEmailVerified()) {
            return;
        }

        if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification code expired");
        }

        if (!user.getVerificationCode().equals(code)) {
            throw new RuntimeException("Invalid verification code");
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        securityDataPort.save(user);
    }

    @Transactional
    public void resendVerificationCode(String email) {
        UserAccount user = securityDataPort.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        String code = generateVerificationCode();
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        securityDataPort.save(user);

        emailPort.sendVerificationEmail(email, code);
    }

    @Transactional
    public void checkAndResendVerificationCodeIfNeeded(String username) {
        UserAccount user = securityDataPort.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEmailVerified()) {
            return;
        }

        if (user.getVerificationCodeExpiresAt() == null
                || user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            String code = generateVerificationCode();
            user.setVerificationCode(code);
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
            securityDataPort.save(user);
            emailPort.sendVerificationEmail(user.getEmail(), code);
        }
    }

    @Transactional
    public void updateUnverifiedEmail(String oldEmail, String newEmail) {
        UserAccount user = securityDataPort.findByEmail(oldEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Cannot change email of a verified account through this endpoint");
        }

        if (securityDataPort.existsByEmail(newEmail)) {
            throw new RuntimeException("Email already exists");
        }

        user.setEmail(newEmail);
        String code = generateVerificationCode();
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        securityDataPort.save(user);

        emailPort.sendVerificationEmail(newEmail, code);
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = 10000000 + random.nextInt(90000000);
        return String.valueOf(code);
    }

    @Transactional
    public UserAccount loginWithGoogle(GoogleAuthRequest req) {
        GoogleTokenVerifier.GoogleClaims claims = googleTokenVerifier.verify(req.idToken());

        UserAccount user = securityDataPort.findByGoogleId(claims.sub())
                .orElseGet(() -> securityDataPort.findByEmail(claims.email())
                        .map(existing -> {
                            existing.setGoogleId(claims.sub());
                            if (existing.getProvider() == AuthProvider.EMAIL) {
                                existing.setProvider(AuthProvider.GOOGLE);
                            }
                            return securityDataPort.save(existing);
                        })
                        .orElseGet(() -> createOAuthUser(
                                claims.sub(), claims.email(), claims.name(), null, AuthProvider.GOOGLE)));

        // La photo de profil Google sert d'avatar par défaut tant que
        // l'utilisateur n'en a pas choisi un manuellement dans les settings.
        if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank())
                && claims.picture() != null && !claims.picture().isBlank()) {
            user.setAvatarUrl(claims.picture());
            user = securityDataPort.save(user);
        }
        return user;
    }

    @Transactional
    public UserAccount loginWithApple(AppleAuthRequest req) {
        AppleTokenVerifier.AppleClaims claims = appleTokenVerifier.verify(req.identityToken());

        return securityDataPort.findByAppleId(claims.sub())
                .orElseGet(() -> {
                    String email = claims.email() != null ? claims.email() : req.email();

                    if (email != null) {
                        return securityDataPort.findByEmail(email)
                                .map(existing -> {
                                    existing.setAppleId(claims.sub());
                                    if (existing.getProvider() == AuthProvider.EMAIL) {
                                        existing.setProvider(AuthProvider.APPLE);
                                    }
                                    return securityDataPort.save(existing);
                                })
                                .orElseGet(() -> createOAuthUser(
                                        claims.sub(), email, req.givenName(), req.familyName(), AuthProvider.APPLE));
                    }
                    // Apple ne fournit pas d'email (relay address désactivé) : on utilise le sub comme email de fallback
                    return createOAuthUser(claims.sub(), claims.sub() + "@privaterelay.appleid.com",
                            req.givenName(), req.familyName(), AuthProvider.APPLE);
                });
    }

    private UserAccount createOAuthUser(String providerId, String email,
            String firstName, String lastName, AuthProvider provider) {
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setUsername(generateUniqueUsername(email));
        user.setRole("USER");
        user.setActive(true);
        user.setEmailVerified(true);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setProvider(provider);
        if (provider == AuthProvider.GOOGLE) user.setGoogleId(providerId);
        else if (provider == AuthProvider.APPLE) user.setAppleId(providerId);
        return securityDataPort.save(user);
    }

    private String generateUniqueUsername(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");
        if (base.length() > 30) base = base.substring(0, 30);
        if (!securityDataPort.existsByUsername(base)) return base;
        SecureRandom rng = new SecureRandom();
        String candidate;
        do {
            candidate = base + "_" + Integer.toHexString(rng.nextInt(0xFFFF));
        } while (securityDataPort.existsByUsername(candidate));
        return candidate;
    }

    public UserAccount findByUsername(String username) {
        return securityDataPort.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Met à jour les champs de profil éditables de l'utilisateur [username].
     * Les paramètres null sont ignorés (mise à jour partielle).
     */
    @Transactional
    public UserAccount updateProfile(String username, String firstName, String lastName,
            String bio, Integer calorieGoal, Boolean notificationsEnabled, String language) {
        UserAccount user = findByUsername(username);
        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (bio != null) user.setBio(bio);
        if (calorieGoal != null && calorieGoal > 0) user.setCalorieGoal(calorieGoal);
        if (notificationsEnabled != null) user.setNotificationsEnabled(notificationsEnabled);
        if (language != null && !language.isBlank()) user.setLanguage(language);
        return securityDataPort.save(user);
    }

    /**
     * Remplace l'URL de l'avatar de l'utilisateur [username] (fichier uploadé
     * via {@code POST /api/user/avatar} ou photo du provider OAuth).
     */
    @Transactional
    public UserAccount updateAvatarUrl(String username, String avatarUrl) {
        UserAccount user = findByUsername(username);
        user.setAvatarUrl(avatarUrl);
        return securityDataPort.save(user);
    }
}

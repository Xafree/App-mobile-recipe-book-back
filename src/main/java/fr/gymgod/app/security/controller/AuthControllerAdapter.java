package fr.gymgod.app.security.controller;

import fr.gymgod.app.security.controller.dto.AppleAuthRequest;
import fr.gymgod.app.security.controller.dto.AuthResponse;
import fr.gymgod.app.security.controller.dto.GoogleAuthRequest;
import fr.gymgod.app.security.controller.dto.UserDto;
import fr.gymgod.common.entities.user.UserAccount;
import fr.gymgod.app.security.service.OrchestratorAuth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthControllerAdapter {

    private final OrchestratorAuth orchestratorAuth;

    @Value("${path.image}")
    private String imageFolder;

    @GetMapping("/user")
    public ResponseEntity<?> getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals("anonymousUser")) {
            String username = authentication.getName();
            UserAccount user = orchestratorAuth.findByUsername(username);

            // Vérifier si le code doit être renvoyé (si expiré)
            if (!user.isEmailVerified()) {
                orchestratorAuth.checkAndResendVerificationCodeIfNeeded(username);
            }

            return ResponseEntity.ok(Map.of(
                    "name", user.getUsername(),
                    "email", user.getEmail(),
                    "authenticated", true,
                    "emailVerified", user.isEmailVerified()));
        }
        return ResponseEntity.ok(Map.of("authenticated", false));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        System.out.println("Registering user: " + request.getUsername());
        try {
            orchestratorAuth.registerUser(request.getUsername(), request.getEmail(), request.getPassword());
            return ResponseEntity.ok("User registered successfully. Please check your email for verification code.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/auth/verify")
    public ResponseEntity<?> verify(@RequestBody VerifyRequest request) {
        try {
            orchestratorAuth.verifyUser(request.getEmail(), request.getCode());
            return ResponseEntity.ok("Email verified successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/auth/resend-code")
    public ResponseEntity<?> resendCode(@RequestBody ResendCodeRequest request) {
        try {
            orchestratorAuth.resendVerificationCode(request.getEmail());
            return ResponseEntity.ok("Verification code resent successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/auth/change-email")
    public ResponseEntity<?> changeEmail(@RequestBody ChangeEmailRequest request) {
        try {
            orchestratorAuth.updateUnverifiedEmail(request.getOldEmail(), request.getNewEmail());
            return ResponseEntity.ok("Email updated and verification code sent successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/auth/google")
    public ResponseEntity<AuthResponse> loginGoogle(
            @RequestBody @Valid GoogleAuthRequest req,
            HttpServletRequest httpRequest) {
        UserAccount user = orchestratorAuth.loginWithGoogle(req);
        createSession(httpRequest, user);
        return ResponseEntity.ok(AuthResponse.of(UserDto.from(user)));
    }

    @PostMapping("/auth/apple")
    public ResponseEntity<AuthResponse> loginApple(
            @RequestBody @Valid AppleAuthRequest req,
            HttpServletRequest httpRequest) {
        UserAccount user = orchestratorAuth.loginWithApple(req);
        createSession(httpRequest, user);
        return ResponseEntity.ok(AuthResponse.of(UserDto.from(user)));
    }

    /**
     * Profil complet de l'utilisateur connecté (DTO enrichi : avatar, bio,
     * préférences). 401 si la session est absente ou expirée.
     */
    @GetMapping("/user/profile")
    public ResponseEntity<UserDto> getProfile() {
        UserAccount user = requireAuthenticatedUser();
        return ResponseEntity.ok(UserDto.from(user));
    }

    /**
     * Mise à jour partielle du profil (les champs null sont ignorés).
     * Retourne le profil mis à jour.
     */
    @PutMapping("/user/profile")
    public ResponseEntity<UserDto> updateProfile(@RequestBody UpdateProfileRequest req) {
        UserAccount current = requireAuthenticatedUser();
        UserAccount updated = orchestratorAuth.updateProfile(
                current.getUsername(), req.getFirstName(), req.getLastName(),
                req.getBio(), req.getCalorieGoal(), req.getNotificationsEnabled(),
                req.getLanguage());
        return ResponseEntity.ok(UserDto.from(updated));
    }

    /**
     * Upload de l'avatar de l'utilisateur connecté.
     * Sauvegarde le fichier dans {@code {path.image}/avatars/} et retourne
     * l'URL relative : {@code { "url": "/api/user/avatar/uuid.jpg" }}.
     */
    @PostMapping("/user/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file) throws IOException {
        UserAccount current = requireAuthenticatedUser();
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fichier vide"));
        }
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.'))
                : ".jpg";
        String filename = UUID.randomUUID() + ext;

        Path dir = Paths.get(imageFolder, "avatars");
        Files.createDirectories(dir);
        Files.write(dir.resolve(filename), file.getBytes());

        String url = "/api/user/avatar/" + filename;
        orchestratorAuth.updateAvatarUrl(current.getUsername(), url);
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * Sert un avatar depuis le disque. Accessible sans session : les clients
     * d'images (CachedNetworkImage) n'envoient pas le cookie SESSION.
     */
    @GetMapping("/user/avatar/{filename:.+}")
    public ResponseEntity<Resource> serveAvatar(@PathVariable String filename) {
        try {
            Path file = Paths.get(imageFolder, "avatars").resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Récupère l'utilisateur de la session courante ou lance une 401
     * (gérée par le GlobalExceptionHandler).
     */
    private UserAccount requireAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            throw new org.springframework.security.access.AccessDeniedException("Non authentifié");
        }
        return orchestratorAuth.findByUsername(authentication.getName());
    }

    private void createSession(HttpServletRequest request, UserAccount user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        session.setAttribute("USER_ID", user.getId().toString());
        session.setAttribute("USER_EMAIL", user.getEmail());
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
    }

    @Data
    public static class VerifyRequest {
        private String email;
        private String code;
    }

    @Data
    public static class ResendCodeRequest {
        private String email;
    }

    @Data
    public static class ChangeEmailRequest {
        private String oldEmail;
        private String newEmail;
    }

    /** Corps de {@code PUT /api/user/profile} — tous les champs sont optionnels. */
    @Data
    public static class UpdateProfileRequest {
        private String firstName;
        private String lastName;
        private String bio;
        private Integer calorieGoal;
        private Boolean notificationsEnabled;
        private String language;
    }
}

package fr.gymgod.app.user.controller;

import fr.gymgod.app.user.domain.record.PublicProfileRecord;
import fr.gymgod.app.user.service.PublicUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Profils publics des utilisateurs.
 * Accessible à tout utilisateur authentifié — aucune vérification d'ownership.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class PublicUserController {

    private final PublicUserService publicUserService;

    /** Retourne le profil public et les recettes publiques de {@code userId}. */
    @GetMapping("/{userId}/public")
    public ResponseEntity<PublicProfileRecord> getPublicProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(publicUserService.getPublicProfile(userId));
    }
}

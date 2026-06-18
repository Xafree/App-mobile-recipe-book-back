package fr.gymgod.app.social.controller;

import fr.gymgod.app.social.domain.record.FollowStateRecord;
import fr.gymgod.app.social.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Gestion du suivi entre utilisateurs.
 * Toutes les routes requièrent une session authentifiée (voir SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/social/follow")
@RequiredArgsConstructor
public class FollowControllerAdapter {

    private final FollowService followService;

    /** Suivre un utilisateur. Idempotent — renvoie l'état courant si déjà suivi. */
    @PostMapping("/{userId}")
    public ResponseEntity<FollowStateRecord> follow(@PathVariable UUID userId) {
        return ResponseEntity.ok(followService.follow(userId));
    }

    /** Ne plus suivre un utilisateur. Idempotent — renvoie l'état courant si pas suivi. */
    @DeleteMapping("/{userId}")
    public ResponseEntity<FollowStateRecord> unfollow(@PathVariable UUID userId) {
        return ResponseEntity.ok(followService.unfollow(userId));
    }

    /** État du suivi entre l'utilisateur courant et un autre. */
    @GetMapping("/state/{userId}")
    public ResponseEntity<FollowStateRecord> getState(@PathVariable UUID userId) {
        return ResponseEntity.ok(followService.getState(userId));
    }
}

package fr.gymgod.app.social.controller;

import fr.gymgod.app.social.domain.record.ConversationRecord;
import fr.gymgod.app.social.domain.record.MessageRecord;
import fr.gymgod.app.social.domain.record.SendMessageRequest;
import fr.gymgod.app.social.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Messagerie directe entre utilisateurs avec suivi mutuel.
 * Toutes les routes requièrent une session authentifiée (voir SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/social/messages")
@RequiredArgsConstructor
public class MessageControllerAdapter {

    private final MessageService messageService;

    /** Envoyer un message à un utilisateur (follow mutuel requis). */
    @PostMapping("/{receiverId}")
    public ResponseEntity<MessageRecord> sendMessage(
            @PathVariable UUID receiverId,
            @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(messageService.sendMessage(receiverId, request.content()));
    }

    /** Messages d'une conversation (les plus récents en premier). Marque les reçus comme lus. */
    @GetMapping("/conversation/{partnerId}")
    public ResponseEntity<List<MessageRecord>> getConversation(
            @PathVariable UUID partnerId,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(messageService.getConversation(partnerId, size));
    }

    /** Liste de toutes les conversations de l'utilisateur courant (résumés). */
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationRecord>> getConversations() {
        return ResponseEntity.ok(messageService.getConversations());
    }
}

package fr.gymgod.app.social.controller;

import fr.gymgod.app.social.service.SseEmitterRegistry;
import fr.gymgod.common.service.SessionSpringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/// Flux SSE de notifications temps réel (NEW_MESSAGE, NEW_FOLLOW, MUTUAL_FOLLOW).
/// Le client Flutter ouvre une seule connexion persistante à l'authentification.
@RestController
@RequestMapping("/api/v1/stream")
@RequiredArgsConstructor
public class SseControllerAdapter {

    private final SseEmitterRegistry registry;
    private final SessionSpringService sessionSpringService;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        UUID userId = UUID.fromString(sessionSpringService.getCurrentUserId());
        return registry.register(userId);
    }
}

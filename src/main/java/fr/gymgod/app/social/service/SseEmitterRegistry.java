package fr.gymgod.app.social.service;

import fr.gymgod.app.social.domain.record.NotificationEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/// Registre thread-safe des connexions SSE actives (une par userId connecté).
/// Un heartbeat ": ping" est envoyé toutes les 20 s pour maintenir la connexion
/// active à travers les proxies/NAT et éviter le timeout async Tomcat (30 s par défaut).
@Component
@Slf4j
public class SseEmitterRegistry {

    private final ConcurrentHashMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();
    private ScheduledExecutorService heartbeatScheduler;

    @PostConstruct
    void startHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sse-heartbeat");
            t.setDaemon(true);
            return t;
        });
        // Envoie un commentaire SSE ": ping" toutes les 20 s — maintient les connexions
        // actives sans déclencher de callback côté Flutter (les commentaires sont ignorés).
        heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeats, 20, 20, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stopHeartbeat() {
        if (heartbeatScheduler != null) heartbeatScheduler.shutdownNow();
    }

    private void sendHeartbeats() {
        emitters.forEach((userId, emitter) -> {
            try {
                synchronized (emitter) {
                    emitter.send(SseEmitter.event().comment("ping"));
                }
            } catch (IOException e) {
                emitters.remove(userId, emitter);
            }
        });
    }

    /// Enregistre (ou remplace) la connexion SSE de l'utilisateur.
    /// Appelé à chaque reconnexion Flutter (ouverture du GET /api/v1/stream).
    public SseEmitter register(UUID userId) {
        SseEmitter old = emitters.remove(userId);
        if (old != null) {
            try { old.complete(); } catch (Exception ignored) {}
        }

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitter.onCompletion(() -> emitters.remove(userId, emitter));
        emitter.onTimeout(()  -> emitters.remove(userId, emitter));
        emitter.onError(e     -> emitters.remove(userId, emitter));
        emitters.put(userId, emitter);
        return emitter;
    }

    /// Pousse un événement à un utilisateur. No-op si l'utilisateur n'est pas connecté.
    public void push(UUID userId, NotificationEvent event) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;
        try {
            // SseEmitter n'est pas thread-safe — synchronisation sur l'instance.
            synchronized (emitter) {
                emitter.send(
                    SseEmitter.event()
                        .name(event.type())
                        .data(event.payload(), MediaType.APPLICATION_JSON)
                );
            }
        } catch (IOException e) {
            emitters.remove(userId, emitter);
        }
    }
}

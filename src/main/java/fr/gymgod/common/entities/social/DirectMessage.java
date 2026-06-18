package fr.gymgod.common.entities.social;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Message direct entre deux utilisateurs.
 * L'envoi n'est autorisé que si les deux se suivent mutuellement (vérifié en service).
 */
@Entity
@Table(name = "direct_message")
@Data
public class DirectMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @Column(nullable = false, length = 2000)
    private String content;

    @CreationTimestamp
    @Column(name = "sent_at", updatable = false)
    private Instant sentAt;

    /** Null si le destinataire n'a pas encore ouvert la conversation. */
    @Column(name = "read_at")
    private Instant readAt;
}

package fr.gymgod.app.social.domain.record;

import fr.gymgod.common.entities.social.DirectMessage;

import java.time.Instant;
import java.util.UUID;

/**
 * Représentation d'un message dans une conversation.
 *
 * @param isFromMe vrai si l'expéditeur est l'utilisateur courant — calculé côté backend
 *                 pour éviter d'exposer l'UUID du user courant au client Flutter.
 */
public record MessageRecord(
    UUID id,
    UUID senderId,
    String content,
    Instant sentAt,
    boolean readByReceiver,
    boolean isFromMe
) {
    /** Construit un [MessageRecord] depuis l'entité, en indiquant si c'est l'envoyeur courant. */
    public static MessageRecord from(DirectMessage m, UUID currentUserId) {
        return new MessageRecord(
            m.getId(),
            m.getSenderId(),
            m.getContent(),
            m.getSentAt(),
            m.getReadAt() != null,
            m.getSenderId().equals(currentUserId)
        );
    }
}

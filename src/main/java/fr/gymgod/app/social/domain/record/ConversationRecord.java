package fr.gymgod.app.social.domain.record;

import java.time.Instant;
import java.util.UUID;

/** Résumé d'une conversation pour la liste des messages. */
public record ConversationRecord(
    UUID partnerId,
    String partnerName,
    String partnerAvatarUrl,
    String lastMessage,
    Instant lastMessageAt,
    boolean hasUnread
) {}

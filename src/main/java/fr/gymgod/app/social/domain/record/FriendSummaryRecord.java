package fr.gymgod.app.social.domain.record;

import java.util.UUID;

/** Résumé d'un ami (suivi mutuel) pour les listes de sélection de contact. */
public record FriendSummaryRecord(UUID userId, String displayName, String avatarUrl) {}

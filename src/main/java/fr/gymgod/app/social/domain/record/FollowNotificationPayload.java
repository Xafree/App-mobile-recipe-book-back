package fr.gymgod.app.social.domain.record;

import java.util.UUID;

/// Payload des événements NEW_FOLLOW et MUTUAL_FOLLOW.
public record FollowNotificationPayload(UUID fromUserId, String fromUserName) {}

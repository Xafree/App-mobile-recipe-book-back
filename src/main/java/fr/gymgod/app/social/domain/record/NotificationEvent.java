package fr.gymgod.app.social.domain.record;

import java.util.UUID;

/// Événement poussé via SSE au client concerné.
/// Le champ [type] est utilisé comme nom d'event SSE côté Flutter.
public record NotificationEvent(String type, Object payload) {

    public static NotificationEvent newMessage(MessageRecord msg) {
        return new NotificationEvent("NEW_MESSAGE", msg);
    }

    public static NotificationEvent newFollow(UUID fromUserId, String fromUserName) {
        return new NotificationEvent("NEW_FOLLOW",
                new FollowNotificationPayload(fromUserId, fromUserName));
    }

    public static NotificationEvent mutualFollow(UUID withUserId, String withUserName) {
        return new NotificationEvent("MUTUAL_FOLLOW",
                new FollowNotificationPayload(withUserId, withUserName));
    }
}

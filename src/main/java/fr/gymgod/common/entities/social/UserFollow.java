package fr.gymgod.common.entities.social;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Relation de suivi entre deux utilisateurs (follower → following).
 * Contrainte unique sur (follower_id, following_id) pour éviter les doublons.
 */
@Entity
@Table(
    name = "user_follow",
    uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "following_id"})
)
@Data
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "follower_id", nullable = false)
    private UUID followerId;

    @Column(name = "following_id", nullable = false)
    private UUID followingId;

    @CreationTimestamp
    @Column(name = "followed_at", updatable = false)
    private Instant followedAt;
}

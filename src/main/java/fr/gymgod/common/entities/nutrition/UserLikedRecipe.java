package fr.gymgod.common.entities.nutrition;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Association entre un utilisateur et une recette qu'il a aimée.
 * Modèle N-N sans entité intermédiaire JPA — clé composite enforced via
 * contrainte unique sur (userId, recipeId).
 */
@Entity
@Table(name = "user_liked_recipe",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "recipe_id"}))
@Data
public class UserLikedRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "recipe_id", nullable = false)
    private UUID recipeId;

    @CreationTimestamp
    @Column(name = "liked_at", updatable = false)
    private Instant likedAt;
}

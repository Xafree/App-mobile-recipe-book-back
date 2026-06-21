package fr.gymgod.common.domain.nutrition;

import fr.gymgod.common.entities.nutrition.Recipe;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, UUID> {
    List<Recipe> findByUserId(UUID userId);

    List<Recipe> findByUserIdAndIsFavoriteTrue(UUID userId);

    /** Toutes les recettes publiques d'un utilisateur, pour son profil public. */
    List<Recipe> findByUserIdAndIsPublicTrueOrderByCreatedAtDesc(UUID userId);

    // ── Feed public — pagination par curseur (keyset) ──────────────────────────

    /** Première page du fil public, sans curseur. */
    @Query("""
            SELECT r FROM Recipe r
            WHERE r.isPublic = true
            AND (:category IS NULL OR r.category = :category)
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<Recipe> findPublicFeedFirstPage(@Param("category") String category, Pageable pageable);

    /** Page suivante du fil public, à partir du curseur (createdAt, id). */
    @Query("""
            SELECT r FROM Recipe r
            WHERE r.isPublic = true
            AND (:category IS NULL OR r.category = :category)
            AND (r.createdAt < :cursorTimestamp
                 OR (r.createdAt = :cursorTimestamp AND r.id < :cursorId))
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<Recipe> findPublicFeedNextPage(@Param("category") String category,
                                        @Param("cursorTimestamp") Instant cursorTimestamp,
                                        @Param("cursorId") UUID cursorId,
                                        Pageable pageable);
}

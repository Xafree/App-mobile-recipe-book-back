package fr.gymgod.app.user.domain.record;

import fr.gymgod.app.feed.domain.record.RecipeSummaryRecord;

import java.util.List;
import java.util.UUID;

/**
 * Profil public d'un utilisateur — nom d'affichage, avatar et recettes publiques.
 * Exposé par {@code GET /api/v1/users/{userId}/public}.
 */
public record PublicProfileRecord(
        UUID userId,
        String displayName,
        String avatarUrl,
        int recipeCount,
        List<RecipeSummaryRecord> recipes
) {}

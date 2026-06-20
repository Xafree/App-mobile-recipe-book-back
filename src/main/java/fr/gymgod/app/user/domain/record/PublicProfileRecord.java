package fr.gymgod.app.user.domain.record;

import fr.gymgod.app.feed.domain.record.RecipeSummaryRecord;

import java.util.List;
import java.util.UUID;

/**
 * Profil public d'un utilisateur — nom d'affichage, avatar, recettes publiques
 * et stats sociales. Exposé par {@code GET /api/v1/users/{userId}/public}.
 */
public record PublicProfileRecord(
        UUID userId,
        String displayName,
        String avatarUrl,
        int recipeCount,
        List<RecipeSummaryRecord> recipes,
        int followersCount,
        int followingCount,
        int likedCount,
        /**
         * Nombre de recettes privées — {@code null} si l'utilisateur courant
         * n'est pas ami (suivi mutuel) avec le propriétaire du profil, pour ne
         * pas exposer cette information à n'importe qui.
         */
        Integer privateRecipeCount
) {}

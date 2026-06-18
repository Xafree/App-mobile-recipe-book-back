package fr.gymgod.app.feed.domain.record;

import fr.gymgod.common.entities.nutrition.Recipe;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Projection légère d'une recette pour le fil public.
 * N'inclut ni ingredients ni étapes pour éviter les requêtes N+1.
 */
public record RecipeSummaryRecord(
        UUID id,
        String title,
        String category,
        String imageUrl,
        Integer servings,
        Integer prepTimeMinutes,
        Boolean isPublic,
        Boolean isFavorite,
        String description,
        BigDecimal caloriesPerServing,
        BigDecimal proteinPerServing,
        BigDecimal carbsPerServing,
        BigDecimal fatPerServing,
        Instant createdAt,
        UUID authorId,
        String authorName,
        Boolean likedByCurrentUser
) {
    public static RecipeSummaryRecord from(Recipe recipe, String authorName, boolean likedByCurrentUser) {
        return from(recipe, authorName, likedByCurrentUser, false);
    }

    public static RecipeSummaryRecord from(Recipe recipe, String authorName, boolean likedByCurrentUser, boolean isOwnRecipe) {
        return new RecipeSummaryRecord(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getCategory(),
                recipe.getImageUrl(),
                recipe.getServings(),
                recipe.getPrepTimeMinutes(),
                recipe.getIsPublic(),
                false, // isFavorite est per-user — toujours false dans le fil public
                recipe.getDescription(),
                recipe.getCaloriesPerServing(),
                recipe.getProteinPerServing(),
                recipe.getCarbsPerServing(),
                recipe.getFatPerServing(),
                recipe.getCreatedAt(),
                isOwnRecipe ? null : recipe.getUserId(), // null = pas de bouton like côté client
                authorName,
                likedByCurrentUser
        );
    }
}

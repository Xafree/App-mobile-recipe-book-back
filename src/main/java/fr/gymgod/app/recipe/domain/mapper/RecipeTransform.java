package fr.gymgod.app.recipe.domain.mapper;

import fr.gymgod.app.recipe.domain.entites.record.RecipeItemRecord;
import fr.gymgod.app.recipe.domain.entites.record.RecipeRecord;
import fr.gymgod.common.entities.nutrition.Recipe;
import fr.gymgod.common.entities.nutrition.RecipeItem;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class RecipeTransform {

    /** Recette de l'utilisateur courant — pas d'auteur affiché, pas de bouton Suivre. */
    public RecipeRecord fromRecipe(Recipe recipe) {
        return fromRecipe(recipe, null, null, false);
    }

    /** Recette d'un autre utilisateur — affiche l'auteur et le bouton Suivre. */
    public RecipeRecord fromRecipe(Recipe recipe, String authorName) {
        return fromRecipe(recipe, recipe != null ? recipe.getUserId() : null, authorName, false);
    }

    /** Recette d'un autre utilisateur, avec état "j'aime" connu (fil, recettes aimées). */
    public RecipeRecord fromRecipe(Recipe recipe, String authorName, boolean likedByCurrentUser) {
        return fromRecipe(recipe, recipe != null ? recipe.getUserId() : null, authorName, likedByCurrentUser);
    }

    /**
     * Variante complète — {@code authorId} doit être {@code null} quand la recette
     * appartient à l'utilisateur courant (cache le bouton Suivre côté client), sinon
     * l'UUID du propriétaire. Utilisée quand le propriétaire de la recette n'est pas
     * forcément l'utilisateur courant (ex. {@code getRecipe} accessible à un ami).
     */
    public RecipeRecord fromRecipe(Recipe recipe, UUID authorId, String authorName, boolean likedByCurrentUser) {
        if (recipe == null) return null;
        return new RecipeRecord(
                recipe.getId(),
                recipe.getUserId(),
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getCategory(),
                recipe.getImageUrl(),
                recipe.getServings(),
                recipe.getPrepTimeMinutes(),
                recipe.getIsPublic(),
                recipe.getIsFavorite(),
                recipe.getCaloriesPerServing(),
                recipe.getProteinPerServing(),
                recipe.getCarbsPerServing(),
                recipe.getFatPerServing(),
                recipe.getIngredients() != null
                        ? recipe.getIngredients().stream()
                                .map(this::fromRecipeItem)
                                .collect(Collectors.toList())
                        : null,
                recipe.getSteps(),
                recipe.getCreatedAt(),
                recipe.getUpdatedAt(),
                authorId,
                authorName,
                likedByCurrentUser);
    }

    public RecipeItemRecord fromRecipeItem(RecipeItem item) {
        if (item == null) return null;
        return new RecipeItemRecord(
                item.getId(),
                item.getPosition(),
                item.getName(),
                item.getImageUrl(),
                item.getQuantity(),
                item.getUnit(),
                item.getCaloriesPer100g(),
                item.getProteinPer100g(),
                item.getCarbsPer100g(),
                item.getFatPer100g(),
                item.getFiberPer100g(),
                item.getSugarPer100g(),
                item.getSaturatedFatPer100g(),
                item.getTransFatPer100g(),
                item.getSodiumPer100g(),
                item.getExternalFoodCode(),
                item.getExternalProductSnapshot(),
                item.getCreatedAt());
    }
}

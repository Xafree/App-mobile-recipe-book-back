package fr.gymgod.app.recipe.domain.mapper;

import fr.gymgod.app.recipe.domain.entites.record.RecipeItemRecord;
import fr.gymgod.app.recipe.domain.entites.record.RecipeRecord;
import fr.gymgod.common.entities.nutrition.Recipe;
import fr.gymgod.common.entities.nutrition.RecipeItem;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class RecipeTransform {

    public RecipeRecord fromRecipe(Recipe recipe) {
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
                recipe.getUpdatedAt());
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
                item.getExternalFoodCode(),
                item.getExternalProductSnapshot(),
                item.getCreatedAt());
    }
}

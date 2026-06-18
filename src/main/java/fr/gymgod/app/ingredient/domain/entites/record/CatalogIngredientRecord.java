package fr.gymgod.app.ingredient.domain.entites.record;

import fr.gymgod.app.recipe.domain.entites.record.NutritionSnapshot;

import java.util.Map;
import java.util.UUID;

/**
 * Ingrédient du catalogue partagé — réponse de
 * {@code POST /api/v1/ingredients} et {@code GET /api/v1/ingredients}.
 */
public record CatalogIngredientRecord(
        UUID id,
        String name,
        String imageUrl,
        String unit,
        NutritionSnapshot nutrition,
        String externalFoodCode,
        Map<String, Object> externalProductSnapshot
) {}

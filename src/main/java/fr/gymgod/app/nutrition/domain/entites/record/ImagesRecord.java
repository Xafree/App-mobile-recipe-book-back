package fr.gymgod.app.nutrition.domain.entites.record;

import java.util.UUID;

public record ImagesRecord(
        UUID id,
        String imageUrl,
        String imageIngredientUrl,
        String imageNutritionUrl
) {
}

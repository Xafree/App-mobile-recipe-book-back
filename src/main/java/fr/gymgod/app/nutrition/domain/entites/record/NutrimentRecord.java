package fr.gymgod.app.nutrition.domain.entites.record;

import java.util.UUID;

public record NutrimentRecord(
        UUID id,
        double energyKcal100g,
        double proteins100g,
        double carbohydrates100g,
        double fat100g,
        double fiber100g,
        double sugars100g,
        double saturatedFat100g
) {
}

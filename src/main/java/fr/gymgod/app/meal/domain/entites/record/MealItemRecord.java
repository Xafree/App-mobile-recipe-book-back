package fr.gymgod.app.meal.domain.entites.record;

import fr.gymgod.app.nutrition.domain.entites.record.ProductRecord;

import java.util.UUID;

public record MealItemRecord(
        UUID id,
        ProductRecord product,
        Double quantity
) {}

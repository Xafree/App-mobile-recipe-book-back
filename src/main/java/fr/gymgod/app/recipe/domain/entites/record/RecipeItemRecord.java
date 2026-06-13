package fr.gymgod.app.recipe.domain.entites.record;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RecipeItemRecord(
        UUID id,
        Integer position,
        String name,
        String imageUrl,
        BigDecimal quantity,
        String unit,
        BigDecimal caloriesPer100g,
        BigDecimal proteinPer100g,
        BigDecimal carbsPer100g,
        BigDecimal fatPer100g,
        BigDecimal fiberPer100g,
        BigDecimal sugarPer100g,
        BigDecimal saturatedFatPer100g,
        BigDecimal transFatPer100g,
        BigDecimal sodiumPer100g,
        String externalFoodCode,
        Map<String, Object> externalProductSnapshot,
        Instant createdAt
) {}

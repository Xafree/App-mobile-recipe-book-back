package fr.gymgod.app.recipe.domain.entites.record;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RecipeRecord(
        UUID id,
        UUID userId,
        String title,
        String description,
        String category,
        String imageUrl,
        Integer servings,
        Integer prepTimeMinutes,
        Boolean isPublic,
        Boolean isFavorite,
        BigDecimal caloriesPerServing,
        BigDecimal proteinPerServing,
        BigDecimal carbsPerServing,
        BigDecimal fatPerServing,
        List<RecipeItemRecord> ingredients,
        List<Map<String, Object>> steps,
        Instant createdAt,
        Instant updatedAt
) {}

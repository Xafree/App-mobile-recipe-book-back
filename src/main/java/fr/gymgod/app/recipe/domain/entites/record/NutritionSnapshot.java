package fr.gymgod.app.recipe.domain.entites.record;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

import static fr.gymgod.common.constants.RecipeConstants.MAX_CALORIES_PER_100G;
import static fr.gymgod.common.constants.RecipeConstants.MAX_MACRO_PER_100G;
import static fr.gymgod.common.constants.RecipeConstants.MAX_SODIUM_MG_PER_100G;

public record NutritionSnapshot(

        @NotNull(message = "Calories per 100g is required")
        @PositiveOrZero(message = "Calories per 100g must not be negative")
        @DecimalMax(value = MAX_CALORIES_PER_100G, message = "Calories per 100g is implausibly high")
        BigDecimal caloriesPer100g,

        @NotNull(message = "Protein per 100g is required")
        @PositiveOrZero(message = "Protein per 100g must not be negative")
        @DecimalMax(value = MAX_MACRO_PER_100G, message = "Protein per 100g cannot exceed 100g")
        BigDecimal proteinPer100g,

        @NotNull(message = "Carbohydrates per 100g is required")
        @PositiveOrZero(message = "Carbohydrates per 100g must not be negative")
        @DecimalMax(value = MAX_MACRO_PER_100G, message = "Carbohydrates per 100g cannot exceed 100g")
        BigDecimal carbsPer100g,

        @NotNull(message = "Fat per 100g is required")
        @PositiveOrZero(message = "Fat per 100g must not be negative")
        @DecimalMax(value = MAX_MACRO_PER_100G, message = "Fat per 100g cannot exceed 100g")
        BigDecimal fatPer100g,

        @PositiveOrZero(message = "Fiber per 100g must not be negative")
        @DecimalMax(value = MAX_MACRO_PER_100G, message = "Fiber per 100g cannot exceed 100g")
        BigDecimal fiberPer100g,

        @PositiveOrZero(message = "Sugar per 100g must not be negative")
        @DecimalMax(value = MAX_MACRO_PER_100G, message = "Sugar per 100g cannot exceed 100g")
        BigDecimal sugarPer100g,

        @PositiveOrZero(message = "Saturated fat per 100g must not be negative")
        @DecimalMax(value = MAX_MACRO_PER_100G, message = "Saturated fat per 100g cannot exceed 100g")
        BigDecimal saturatedFatPer100g,

        @PositiveOrZero(message = "Trans fat per 100g must not be negative")
        @DecimalMax(value = MAX_MACRO_PER_100G, message = "Trans fat per 100g cannot exceed 100g")
        BigDecimal transFatPer100g,

        @PositiveOrZero(message = "Sodium per 100g must not be negative")
        @DecimalMax(value = MAX_SODIUM_MG_PER_100G, message = "Sodium per 100g is implausibly high")
        BigDecimal sodiumPer100g
) {}

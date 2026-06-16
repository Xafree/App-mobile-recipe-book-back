package fr.gymgod.app.recipe.domain.entites.record;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

import static fr.gymgod.common.constants.RecipeConstants.MAX_CALCIUM_MG_PER_100G;
import static fr.gymgod.common.constants.RecipeConstants.MAX_CALORIES_PER_100G;
import static fr.gymgod.common.constants.RecipeConstants.MAX_CHOLESTEROL_MG_PER_100G;
import static fr.gymgod.common.constants.RecipeConstants.MAX_IRON_MG_PER_100G;
import static fr.gymgod.common.constants.RecipeConstants.MAX_MACRO_PER_100G;
import static fr.gymgod.common.constants.RecipeConstants.MAX_POTASSIUM_MG_PER_100G;
import static fr.gymgod.common.constants.RecipeConstants.MAX_PREPARED_RATIO;
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

        @PositiveOrZero(message = "Cholesterol per 100g must not be negative")
        @DecimalMax(value = MAX_CHOLESTEROL_MG_PER_100G, message = "Cholesterol per 100g is implausibly high")
        BigDecimal cholesterolPer100g,

        @PositiveOrZero(message = "Sodium per 100g must not be negative")
        @DecimalMax(value = MAX_SODIUM_MG_PER_100G, message = "Sodium per 100g is implausibly high")
        BigDecimal sodiumPer100g,

        @PositiveOrZero(message = "Potassium per 100g must not be negative")
        @DecimalMax(value = MAX_POTASSIUM_MG_PER_100G, message = "Potassium per 100g is implausibly high")
        BigDecimal potassiumPer100g,

        @PositiveOrZero(message = "Calcium per 100g must not be negative")
        @DecimalMax(value = MAX_CALCIUM_MG_PER_100G, message = "Calcium per 100g is implausibly high")
        BigDecimal calciumPer100g,

        @PositiveOrZero(message = "Iron per 100g must not be negative")
        @DecimalMax(value = MAX_IRON_MG_PER_100G, message = "Iron per 100g is implausibly high")
        BigDecimal ironPer100g,

        // ── Valeurs "préparées" (optionnelles) ──────────────────────────────
        // Renseignées uniquement si l'étiquette fournit une seconde colonne de
        // valeurs pour le produit "tel que préparé/cuit/reconstitué" (voir
        // OcrNutritionRecord). null si l'ingrédient n'a qu'une seule colonne.

        @Positive(message = "Prepared ratio must be greater than zero")
        @DecimalMax(value = MAX_PREPARED_RATIO, message = "Prepared ratio is implausibly high")
        BigDecimal preparedRatio,

        @PositiveOrZero(message = "Calories per 100g prepared must not be negative")
        @DecimalMax(value = MAX_CALORIES_PER_100G, message = "Calories per 100g prepared is implausibly high")
        BigDecimal caloriesPreparedPer100g,

        @PositiveOrZero(message = "Protein per 100g prepared must not be negative")
        @DecimalMax(value = MAX_MACRO_PER_100G, message = "Protein per 100g prepared cannot exceed 100g")
        BigDecimal proteinPreparedPer100g,

        @PositiveOrZero(message = "Carbohydrates per 100g prepared must not be negative")
        @DecimalMax(value = MAX_MACRO_PER_100G, message = "Carbohydrates per 100g prepared cannot exceed 100g")
        BigDecimal carbsPreparedPer100g,

        @PositiveOrZero(message = "Fat per 100g prepared must not be negative")
        @DecimalMax(value = MAX_MACRO_PER_100G, message = "Fat per 100g prepared cannot exceed 100g")
        BigDecimal fatPreparedPer100g
) {}

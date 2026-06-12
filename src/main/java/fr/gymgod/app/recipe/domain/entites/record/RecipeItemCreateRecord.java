package fr.gymgod.app.recipe.domain.entites.record;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;

import static fr.gymgod.common.constants.RecipeConstants.MAX_EXTERNAL_FOOD_CODE_LENGTH;
import static fr.gymgod.common.constants.RecipeConstants.UNIT_PATTERN;

public record RecipeItemCreateRecord(

        @NotBlank(message = "Le nom de l'ingrédient est obligatoire")
        @Size(max = 150, message = "Le nom de l'ingrédient ne doit pas dépasser 150 caractères")
        String name,

        String imageUrl,

        @NotNull(message = "La quantité est obligatoire")
        @Positive(message = "La quantité doit être supérieure à zéro")
        BigDecimal quantity,

        @NotBlank(message = "L'unité est obligatoire")
        @Pattern(regexp = UNIT_PATTERN, message = "L'unité doit être g, ml ou piece")
        String unit,

        @NotNull(message = "Les valeurs nutritionnelles sont obligatoires")
        @Valid
        NutritionSnapshot nutrition,

        @Size(max = MAX_EXTERNAL_FOOD_CODE_LENGTH, message = "Le code produit est trop long")
        String externalFoodCode,

        Map<String, Object> externalProductSnapshot
) {}

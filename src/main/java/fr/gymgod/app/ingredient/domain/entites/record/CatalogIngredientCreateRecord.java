package fr.gymgod.app.ingredient.domain.entites.record;

import fr.gymgod.app.recipe.domain.entites.record.NutritionSnapshot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

import static fr.gymgod.common.constants.RecipeConstants.MAX_EXTERNAL_FOOD_CODE_LENGTH;
import static fr.gymgod.common.constants.RecipeConstants.MAX_TITLE_LENGTH;
import static fr.gymgod.common.constants.RecipeConstants.UNIT_PATTERN;

/**
 * Payload de sauvegarde d'un ingrédient du catalogue partagé (saisie manuelle
 * ou scan OCR) — {@code POST /api/v1/ingredients}.
 *
 * <p>Seul {@code name} est obligatoire ({@code externalFoodCode}/
 * {@code externalProductSnapshot} sont {@code null} pour une saisie
 * manuelle, voir Javadoc de {@code IngredientForm} côté Flutter).
 */
public record CatalogIngredientCreateRecord(

        @NotBlank(message = "Le nom de l'ingrédient est obligatoire")
        @Size(max = MAX_TITLE_LENGTH, message = "Le nom de l'ingrédient ne doit pas dépasser 150 caractères")
        String name,

        String imageUrl,

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

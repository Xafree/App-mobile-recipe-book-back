package fr.gymgod.app.recipe.domain.entites.record;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;

import static fr.gymgod.common.constants.RecipeConstants.*;

public record RecipeCreateRecord(

        @NotBlank(message = "Le titre de la recette est obligatoire")
        @Size(max = MAX_TITLE_LENGTH, message = "Le titre ne doit pas dépasser 150 caractères")
        String title,

        String description,

        @NotBlank(message = "La catégorie est obligatoire")
        @Size(max = MAX_CATEGORY_LENGTH, message = "La catégorie ne doit pas dépasser 50 caractères")
        String category,

        String imageUrl,

        @NotNull(message = "Le nombre de portions est obligatoire")
        @Min(value = MIN_SERVINGS, message = "Le nombre de portions doit être au moins 1")
        @Max(value = MAX_SERVINGS, message = "Le nombre de portions ne doit pas dépasser 12")
        Integer servings,

        Integer prepTimeMinutes,

        @NotNull(message = "isPublic est obligatoire")
        Boolean isPublic,

        Boolean isFavorite,

        @NotEmpty(message = "La recette doit contenir au moins un ingrédient")
        @Valid
        List<RecipeItemCreateRecord> ingredients,

        /** Blocs de préparation (étapes/cuissons/repos). Optionnel. */
        List<Map<String, Object>> steps
) {}

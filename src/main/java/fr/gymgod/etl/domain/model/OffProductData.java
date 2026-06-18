package fr.gymgod.etl.domain.model;

import fr.gymgod.common.entities.nutrition.Glucide;
import fr.gymgod.common.entities.nutrition.Nutriment;
import fr.gymgod.common.entities.nutrition.Vitamin;

/**
 * Données produit récupérées en direct via l'API produit OpenFoodFacts pour
 * un code-barres absent de la base locale (cf. {@code OffProductImportService}).
 */
public record OffProductData(
        String productName,
        String quantity,
        String ingredientsText,
        int nutriscore,
        String nutriscoreGrade,
        Nutriment nutriment,
        Glucide glucide,
        Vitamin vitamin) {
}

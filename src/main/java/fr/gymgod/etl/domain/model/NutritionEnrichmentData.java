package fr.gymgod.etl.domain.model;

import fr.gymgod.common.entities.nutrition.Glucide;
import fr.gymgod.common.entities.nutrition.Nutriment;
import fr.gymgod.common.entities.nutrition.Vitamin;

/**
 * Données nutritionnelles récupérées via l'API produit OpenFoodFacts pour
 * compléter un {@link fr.gymgod.common.entities.nutrition.Product} dont le
 * flag {@code nutritionDataIncomplete} est à {@code true}.
 */
public record NutritionEnrichmentData(Nutriment nutriment, Glucide glucide, Vitamin vitamin) {
}

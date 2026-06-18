package fr.gymgod.app.nutrition.domain.entites.record;

/**
 * Résultats de l'extraction des valeurs nutritionnelles d'une même étiquette
 * par les trois implémentations de {@code OcrLabelParsingPort}, pour
 * comparaison manuelle (voir {@code POST /nutrition/ocr/parse-label/compare}).
 */
public record OcrLabelCompareResult(
        OcrNutritionRecord llm,
        OcrNutritionRecord regex,
        OcrNutritionRecord python
) {
}

package fr.gymgod.app.nutrition.domain.entites.record;

/**
 * Valeurs nutritionnelles telles qu'imprimées sur l'étiquette (pour
 * {@code servingSizeG} grammes/ml), extraites par le LLM depuis le texte OCR.
 * Champs {@code null} si introuvables sur l'étiquette — le client les traite
 * comme "à compléter manuellement".
 *
 * <p>Aucune conversion vers 100 g/ml n'est faite côté serveur : le client
 * affiche ces valeurs telles quelles (comparables à l'étiquette physique) et
 * les convertit vers 100 g/ml au moment de la validation de l'ajout.
 *
 * <p>Les champs {@code *Prepared*} portent les valeurs de la seconde colonne
 * éventuelle de l'étiquette ("tel que préparé/cuit/reconstitué"), pour
 * {@code preparedServingSizeG}. Tous {@code null} si l'étiquette n'a qu'une
 * seule colonne de valeurs — le client masque alors la colonne "Préparé".
 */
public record OcrNutritionRecord(
        Double servingSizeG,
        Double energyKcalServing,
        Double proteinsServing,
        Double carbohydratesServing,
        Double fatServing,
        Double sugarsServing,
        Double saturatedFatServing,
        Double transFatServing,
        Double fiberServing,
        Double cholesterolServing,
        Double sodiumServing,
        Double potassiumServing,
        Double calciumServing,
        Double ironServing,
        Double preparedServingSizeG,
        Double energyKcalPreparedServing,
        Double proteinsPreparedServing,
        Double carbohydratesPreparedServing,
        Double fatPreparedServing,
        Double sugarsPreparedServing,
        Double saturatedFatPreparedServing,
        Double transFatPreparedServing,
        Double fiberPreparedServing,
        Double sodiumPreparedServing
) {
}

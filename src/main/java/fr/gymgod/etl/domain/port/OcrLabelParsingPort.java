package fr.gymgod.etl.domain.port;

import fr.gymgod.etl.domain.model.OcrNutritionData;

import java.util.Optional;

/**
 * Port pour l'extraction structurée des valeurs nutritionnelles à partir du
 * texte brut OCR d'une étiquette (scan à la demande côté client, ML Kit).
 */
public interface OcrLabelParsingPort {

    /**
     * @param rawText texte brut extrait par OCR de l'étiquette nutritionnelle
     * @return les valeurs extraites, ou {@link Optional#empty()} si l'appel
     *         LLM a échoué (le client retombe alors sur un formulaire vierge)
     */
    Optional<OcrNutritionData> parseLabel(String rawText);
}

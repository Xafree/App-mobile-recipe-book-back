package fr.gymgod.app.nutrition.domain.entites.record;

/**
 * Texte brut extrait par OCR (côté client, ML Kit) d'une étiquette
 * nutritionnelle, à transmettre au LLM pour extraction structurée.
 */
public record OcrLabelParseRequest(String rawText) {
}

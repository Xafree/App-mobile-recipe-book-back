package fr.gymgod.common.image;

/**
 * Résultat du traitement d'une image uploadée : octets prêts à écrire sur
 * disque et extension de fichier à utiliser (peut différer de l'extension
 * d'origine si {@link ImageResizer} a dû réencoder l'image).
 */
public record ProcessedImage(byte[] bytes, String extension) {
}

package fr.gymgod.common.entities.nutrition;

/**
 * Origine d'une entrée de {@link ReferenceFood} : table de composition
 * nutritionnelle officielle utilisée pour fiabiliser les aliments bruts/génériques.
 */
public enum ReferenceFoodSource {
    /** CIQUAL (ANSES, France/UE). */
    CIQUAL,
    /** USDA FoodData Central — Foundation Foods (US). */
    USDA
}

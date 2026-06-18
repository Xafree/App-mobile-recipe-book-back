package fr.gymgod.etl.gateway.ocr;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Dictionnaire de libellés FR/EN et expressions régulières utilisés par
 * {@link fr.gymgod.etl.gateway.OcrLabelParsingRegexGateway} pour repérer les
 * lignes d'une étiquette nutritionnelle et en extraire les valeurs.
 */
public final class NutritionLabelDictionary {

    private NutritionLabelDictionary() {
    }

    /** Unité canonique d'un champ, ou unité brute capturée par {@link #NUMBER_UNIT}. */
    public enum Unit {
        NONE, G, MG, MCG, KCAL, KJ
    }

    /**
     * Un champ nutritionnel : sa clé interne, le motif identifiant sa ligne sur
     * l'étiquette (FR/EN) et l'unité canonique attendue pour ses valeurs.
     */
    public record FieldSpec(String key, Pattern labelPattern, Unit unit) {
    }

    /**
     * Ordre important : les motifs les plus spécifiques (acides gras saturés,
     * trans, sucres, fibres) sont vérifiés avant les motifs génériques
     * (lipides, glucides) qu'ils pourraient sinon faire matcher en premier.
     */
    public static final List<FieldSpec> FIELDS = List.of(
            new FieldSpec("energy", Pattern.compile("(?i)valeur\\s+(é|e)nerg|(é|e)nerg|calor"), Unit.KCAL),
            new FieldSpec("saturatedFat", Pattern.compile("(?i)satur"), Unit.G),
            new FieldSpec("transFat", Pattern.compile("(?i)\\btrans\\b"), Unit.G),
            new FieldSpec("sugars", Pattern.compile("(?i)sucre|sugar"), Unit.G),
            new FieldSpec("fiber", Pattern.compile("(?i)fibre|fiber"), Unit.G),
            new FieldSpec("carbohydrates", Pattern.compile("(?i)glucide|carbohydrate"), Unit.G),
            new FieldSpec("fat", Pattern.compile("(?i)lipide|mati[èe]res?\\s+grasses|\\bfat\\b"), Unit.G),
            new FieldSpec("proteins", Pattern.compile("(?i)prot[ée]ine|protein"), Unit.G),
            new FieldSpec("cholesterol", Pattern.compile("(?i)cholest"), Unit.MG),
            new FieldSpec("sodium", Pattern.compile("(?i)\\bsodium\\b"), Unit.MG),
            new FieldSpec("salt", Pattern.compile("(?i)\\bsel\\b|\\bsalt\\b"), Unit.G),
            new FieldSpec("potassium", Pattern.compile("(?i)potassium"), Unit.MG),
            new FieldSpec("calcium", Pattern.compile("(?i)calcium"), Unit.MG),
            new FieldSpec("iron", Pattern.compile("(?i)\\bfer\\b|\\biron\\b"), Unit.MG));

    /** Nombre (virgule ou point décimal) suivi d'une unité optionnelle. */
    public static final Pattern NUMBER_UNIT = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(kcal|kj|mg|µg|mcg|g)?", Pattern.CASE_INSENSITIVE);

    /** Poids/volume de portion : "per 100 g", "pour 1/2 tasse (125 g)", "30g"... */
    public static final Pattern WEIGHT = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(kg|g|l|ml)\\b", Pattern.CASE_INSENSITIVE);

    /**
     * Mot-clé signalant une portion "préparée" explicite (ex. "pour 1/4 tasse
     * sec (28 g) / 140 g préparé(e)(s)", "Per 1/4 cup dry (28 g) / 140 g
     * prepared").
     */
    public static final Pattern PREPARED_KEYWORD = Pattern.compile(
            "pr[ée]par[ée]{1,2}s?|prepared", Pattern.CASE_INSENSITIVE);
}

package fr.gymgod.etl.gateway;

import fr.gymgod.etl.domain.model.OcrNutritionData;
import fr.gymgod.etl.domain.port.OcrLabelParsingPort;
import fr.gymgod.etl.gateway.ocr.NutritionLabelDictionary;
import fr.gymgod.etl.gateway.ocr.NutritionLabelDictionary.FieldSpec;
import fr.gymgod.etl.gateway.ocr.NutritionLabelDictionary.Unit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * Extraction des valeurs nutritionnelles depuis le texte brut OCR d'une
 * étiquette par dictionnaire de libellés FR/EN + expressions régulières,
 * sans appel LLM.
 *
 * <p>Variante de comparaison de {@link OcrLabelParsingOllamaGateway} (voir
 * l'endpoint {@code /ocr/parse-label/compare}) — les règles d'extraction et
 * de conversion (kJ→kcal, sel→sodium, détection de la seconde colonne
 * "préparé") suivent celles décrites dans le prompt de ce dernier.
 */
@Service
@Qualifier("ocrRegex")
@Slf4j
public class OcrLabelParsingRegexGateway implements OcrLabelParsingPort {

    private static final double DEFAULT_SERVING_SIZE_G = 100.0;
    private static final double KJ_TO_KCAL_DIVISOR = 4.184;
    private static final double SALT_TO_SODIUM_FACTOR = 1000.0 / 2.5;
    private static final int MAX_NUMBERS_PER_LINE = 2;
    private static final int TWO_COLUMN_LINE_THRESHOLD = 2;

    @Override
    public Optional<OcrNutritionData> parseLabel(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }

        Map<String, double[]> values = new HashMap<>();
        int twoColumnLines = 0;
        for (String line : rawText.split("\\R")) {
            twoColumnLines += extractLine(line, values);
        }
        boolean prepared = twoColumnLines >= TWO_COLUMN_LINE_THRESHOLD;

        double servingSize = extractServingSize(rawText);
        Double preparedServingSize = extractPreparedServing(rawText);

        return Optional.of(buildResult(values, servingSize, preparedServingSize, prepared));
    }

    /**
     * Repère le premier champ du dictionnaire dont le libellé apparaît sur la
     * ligne et enregistre sa/ses valeur(s) (converties dans l'unité canonique
     * du champ) dans {@code values}, sous la forme {@code [serving, prepared]}
     * ({@code prepared} = {@link Double#NaN} si absent).
     *
     * @return 1 si la ligne porte deux valeurs de même unité brute (signal
     *         d'une seconde colonne "préparé"), 0 sinon.
     */
    private int extractLine(String line, Map<String, double[]> values) {
        for (FieldSpec spec : NutritionLabelDictionary.FIELDS) {
            Matcher label = spec.labelPattern().matcher(line);
            if (!label.find()) {
                continue;
            }

            List<double[]> numbers = extractNumbers(line.substring(label.end()));
            if (numbers.isEmpty()) {
                return 0;
            }

            double serving = toCanonical(numbers.get(0), spec.unit());
            double prepared = Double.NaN;
            boolean sameUnitPair = numbers.size() == 2 && numbers.get(0)[1] == numbers.get(1)[1];
            if (sameUnitPair) {
                prepared = toCanonical(numbers.get(1), spec.unit());
            }
            values.put(spec.key(), new double[] { serving, prepared });
            return sameUnitPair ? 1 : 0;
        }
        return 0;
    }

    /** Extrait jusqu'à {@value #MAX_NUMBERS_PER_LINE} paires {@code [valeur, unité brute]}. */
    private List<double[]> extractNumbers(String remainder) {
        List<double[]> numbers = new java.util.ArrayList<>();
        Matcher matcher = NutritionLabelDictionary.NUMBER_UNIT.matcher(remainder);
        while (matcher.find() && numbers.size() < MAX_NUMBERS_PER_LINE) {
            double value = Double.parseDouble(matcher.group(1).replace(',', '.'));
            Unit unit = parseUnit(matcher.group(2));
            numbers.add(new double[] { value, unit.ordinal() });
        }
        return numbers;
    }

    private Unit parseUnit(String raw) {
        if (raw == null) {
            return Unit.NONE;
        }
        return switch (raw.toLowerCase()) {
            case "g" -> Unit.G;
            case "mg" -> Unit.MG;
            case "µg", "mcg" -> Unit.MCG;
            case "kcal" -> Unit.KCAL;
            case "kj" -> Unit.KJ;
            default -> Unit.NONE;
        };
    }

    /** Convertit {@code [valeur, unité brute]} vers l'unité canonique du champ. */
    private double toCanonical(double[] numberUnit, Unit canonical) {
        double value = numberUnit[0];
        Unit matched = Unit.values()[(int) numberUnit[1]];
        if (matched == Unit.NONE || matched == canonical) {
            return value;
        }
        return switch (canonical) {
            case G -> matched == Unit.MG ? value / 1000.0 : value;
            case MG -> switch (matched) {
                case G -> value * 1000.0;
                case MCG -> value / 1000.0;
                default -> value;
            };
            case KCAL -> matched == Unit.KJ ? value / KJ_TO_KCAL_DIVISOR : value;
            default -> value;
        };
    }

    /**
     * Cherche le poids/volume de portion "telle qu'imprimée" en tête
     * d'étiquette (ex. "per 1/2 cup (125 g)", "pour 100 g").
     */
    private double extractServingSize(String rawText) {
        String header = rawText.lines().limit(2).reduce("", (a, b) -> a + " " + b);
        Matcher matcher = NutritionLabelDictionary.WEIGHT.matcher(header);
        if (matcher.find()) {
            double grams = toGrams(matcher);
            if (grams > 0) {
                return grams;
            }
        }
        return DEFAULT_SERVING_SIZE_G;
    }

    /**
     * Cherche une phrase explicite de portion "préparée" (ex. "pour 1/4
     * tasse sec (28 g) / 140 g préparé", "Per 1/4 cup dry (28 g) / 140 g
     * prepared"). La portion préparée n'est renseignée que si une telle
     * phrase contenant le mot "préparé"/"prepared" est présente sur une
     * ligne — sinon {@code null}.
     */
    private Double extractPreparedServing(String rawText) {
        for (String line : rawText.split("\\R")) {
            if (!NutritionLabelDictionary.PREPARED_KEYWORD.matcher(line).find()) {
                continue;
            }
            Matcher matcher = NutritionLabelDictionary.WEIGHT.matcher(line);
            double grams = Double.NaN;
            while (matcher.find()) {
                grams = toGrams(matcher);
            }
            if (!Double.isNaN(grams) && grams > 0) {
                return grams;
            }
        }
        return null;
    }

    private double toGrams(Matcher weightMatch) {
        double value = Double.parseDouble(weightMatch.group(1).replace(',', '.'));
        String unit = weightMatch.group(2).toLowerCase();
        return switch (unit) {
            case "kg", "l" -> value * 1000.0;
            default -> value;
        };
    }

    private OcrNutritionData buildResult(Map<String, double[]> values, double servingSize, Double preparedServingSize,
            boolean prepared) {
        double sodiumServing = orFromSalt(values, 0);
        double sodiumPrepared = orFromSalt(values, 1);

        return new OcrNutritionData(
                servingSize,
                field(values, "energy", 0),
                field(values, "proteins", 0),
                field(values, "carbohydrates", 0),
                field(values, "fat", 0),
                field(values, "sugars", 0),
                field(values, "saturatedFat", 0),
                field(values, "transFat", 0),
                field(values, "fiber", 0),
                field(values, "cholesterol", 0),
                nanToNull(sodiumServing),
                field(values, "potassium", 0),
                field(values, "calcium", 0),
                field(values, "iron", 0),
                preparedServingSize,
                prepared ? field(values, "energy", 1) : null,
                prepared ? field(values, "proteins", 1) : null,
                prepared ? field(values, "carbohydrates", 1) : null,
                prepared ? field(values, "fat", 1) : null,
                prepared ? field(values, "sugars", 1) : null,
                prepared ? field(values, "saturatedFat", 1) : null,
                prepared ? field(values, "transFat", 1) : null,
                prepared ? field(values, "fiber", 1) : null,
                prepared ? nanToNull(sodiumPrepared) : null);
    }

    /** Valeur de {@code values.get(key)[index]}, ou {@code null} si absente/NaN. */
    private Double field(Map<String, double[]> values, String key, int index) {
        double[] value = values.get(key);
        if (value == null || Double.isNaN(value[index])) {
            return null;
        }
        return value[index];
    }

    /**
     * Valeur de sodium à l'index donné : celle relevée directement
     * ("sodium"), ou convertie depuis le sel ("sel"/"salt") si seul celui-ci
     * est présent sur l'étiquette.
     */
    private double orFromSalt(Map<String, double[]> values, int index) {
        Double sodium = field(values, "sodium", index);
        if (sodium != null) {
            return sodium;
        }
        Double salt = field(values, "salt", index);
        return salt != null ? salt * SALT_TO_SODIUM_FACTOR : Double.NaN;
    }

    private Double nanToNull(double value) {
        return Double.isNaN(value) ? null : value;
    }
}

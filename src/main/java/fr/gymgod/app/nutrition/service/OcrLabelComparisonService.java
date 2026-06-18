package fr.gymgod.app.nutrition.service;

import fr.gymgod.app.nutrition.domain.entites.record.OcrLabelCompareResult;
import fr.gymgod.app.nutrition.domain.entites.record.OcrNutritionRecord;
import fr.gymgod.app.nutrition.domain.mapper.OcrNutritionTransform;
import fr.gymgod.etl.domain.port.OcrLabelParsingPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Exécute les trois implémentations de {@link OcrLabelParsingPort} (LLM
 * Ollama, règles regex Java, service Python ocr-label-parser) sur le même
 * texte OCR, pour comparaison manuelle — n'est utilisé que par l'endpoint de
 * comparaison, le flux de production ({@link OrchestratorNutrition#parseOcrLabel})
 * utilise le service Python (sans LLM).
 */
@Service
public class OcrLabelComparisonService {

    private final OcrLabelParsingPort llmGateway;
    private final OcrLabelParsingPort regexGateway;
    private final OcrLabelParsingPort pythonGateway;
    private final OcrNutritionTransform ocrNutritionTransform;

    public OcrLabelComparisonService(
            @Qualifier("ocrOllama") OcrLabelParsingPort llmGateway,
            @Qualifier("ocrRegex") OcrLabelParsingPort regexGateway,
            @Qualifier("ocrPython") OcrLabelParsingPort pythonGateway,
            OcrNutritionTransform ocrNutritionTransform) {
        this.llmGateway = llmGateway;
        this.regexGateway = regexGateway;
        this.pythonGateway = pythonGateway;
        this.ocrNutritionTransform = ocrNutritionTransform;
    }

    public OcrLabelCompareResult compare(String rawText) {
        var llm = llmGateway.parseLabel(rawText).map(ocrNutritionTransform::toRecord)
                .orElseGet(ocrNutritionTransform::empty);
        var regex = regexGateway.parseLabel(rawText).map(ocrNutritionTransform::toRecord)
                .orElseGet(ocrNutritionTransform::empty);
        var python = pythonGateway.parseLabel(rawText).map(ocrNutritionTransform::toRecord)
                .orElseGet(ocrNutritionTransform::empty);
        return new OcrLabelCompareResult(llm, regex, python);
    }

    /**
     * Résultat du seul gateway Python (ocr-label-parser), au même format que
     * {@code /ocr/parse-label} (LLM) — permet de pointer temporairement
     * l'application mobile sur ce parseur pour le tester sur des scans réels.
     */
    public OcrNutritionRecord python(String rawText) {
        return pythonGateway.parseLabel(rawText).map(ocrNutritionTransform::toRecord)
                .orElseGet(ocrNutritionTransform::empty);
    }
}

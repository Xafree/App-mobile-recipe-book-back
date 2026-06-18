package fr.gymgod.etl.gateway;

import fr.gymgod.etl.domain.model.OcrNutritionData;
import fr.gymgod.etl.domain.port.OcrLabelParsingPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Extraction des valeurs nutritionnelles via le service Python
 * {@code ocr-label-parser} (FastAPI), appelé en HTTP sur {@code ocr.python.url}.
 *
 * <p>Implémentation par défaut ({@code @Primary}) de {@link OcrLabelParsingPort}
 * — utilisée par le flux de production ({@code OrchestratorNutrition#parseOcrLabel}),
 * sans dépendance à un LLM. Voir aussi {@link OcrLabelParsingOllamaGateway} et
 * {@link OcrLabelParsingRegexGateway} (endpoint de comparaison
 * {@code /ocr/parse-label/compare}).
 */
@Service
@Primary
@Qualifier("ocrPython")
@Slf4j
public class OcrLabelParsingPythonGateway implements OcrLabelParsingPort {

    /** Même limite que côté service Python (modèle Pydantic) : défense en
     * profondeur, évite un appel réseau pour un payload déjà invalide. */
    private static final int MAX_RAW_TEXT_LENGTH = 10_000;

    private final RestClient restClient;

    public OcrLabelParsingPythonGateway(@Value("${ocr.python.url}") String url,
            @Value("${ocr.python.timeout:10}") int timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout * 1000);
        factory.setReadTimeout(timeout * 1000);

        this.restClient = RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .build();
    }

    private record ParseLabelRequest(String rawText) {
    }

    @Override
    public Optional<OcrNutritionData> parseLabel(String rawText) {
        if (rawText == null || rawText.isBlank() || rawText.length() > MAX_RAW_TEXT_LENGTH) {
            return Optional.empty();
        }

        try {
            OcrNutritionData data = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ParseLabelRequest(rawText))
                    .retrieve()
                    .body(OcrNutritionData.class);
            return Optional.ofNullable(data);
        } catch (Exception e) {
            log.warn("Erreur lors de l'appel au service Python ocr-label-parser: {} - {}",
                    e.getClass().getSimpleName(), e.getMessage());
            return Optional.empty();
        }
    }
}

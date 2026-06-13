package fr.gymgod.etl.gateway.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class LlmClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String url;
    private final boolean enabled;

    private static final long MAX_RESPONSE_SIZE = 20 * 1024 * 1024; // 20 MB

    public LlmClient(@Value("${ai.url}") String url,
            @Value("${ai.model}") String model,
            @Value("${ai.enabled}") boolean enabled,
            @Value("${ai.timeout:30}") int timeout,
            ObjectMapper objectMapper) {
        this.url = url;
        this.model = model;
        this.enabled = enabled;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout * 1000);
        factory.setReadTimeout(timeout * 1000);

        this.restClient = RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .build();
    }

    public String call(String prompt) {
        return call(prompt, null);
    }

    /**
     * Appelle le LLM. {@code params} peut contenir :
     * <ul>
     * <li>{@code format} : "json" (défaut) ou un schéma JSON ({@code Map}) pour
     * contraindre la structure de la réponse</li>
     * <li>{@code temperature} : transmis dans les {@code options} Ollama</li>
     * <li>{@code options} : autres paramètres du modèle (num_ctx,
     * num_predict...), fusionnés dans les {@code options} Ollama</li>
     * </ul>
     */
    public String call(String prompt, java.util.Map<String, Object> params) {
        if (!enabled) {
            return null;
        }

        Object format = "json";
        java.util.Map<String, Object> ollamaOptions = new java.util.HashMap<>();

        if (params != null) {
            if (params.containsKey("format")) {
                format = params.get("format");
            }
            if (params.get("options") instanceof java.util.Map<?, ?> nestedOptions) {
                nestedOptions.forEach((key, value) -> ollamaOptions.put(String.valueOf(key), value));
            }
            if (params.containsKey("temperature")) {
                ollamaOptions.put("temperature", params.get("temperature"));
            }
        }

        LlmRequest request = LlmRequest.builder()
                .model(model)
                .prompt(prompt)
                .format(format)
                .stream(false)
                .options(ollamaOptions)
                .build();

        try {
            return restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange((req, res) -> {
                        if (res.getStatusCode().isError()) {
                            log.warn("LLM API returned error status: {}", res.getStatusCode());
                            return null;
                        }

                        try (InputStream is = res.getBody()) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            long totalBytes = 0;

                            while ((bytesRead = is.read(buffer)) != -1) {
                                totalBytes += bytesRead;
                                if (totalBytes > MAX_RESPONSE_SIZE) {
                                    throw new IOException(
                                            "LLM response exceeded maximum size of " + MAX_RESPONSE_SIZE + " bytes");
                                }
                                baos.write(buffer, 0, bytesRead);
                            }

                            if (totalBytes == 0) {
                                return null;
                            }

                            String responseBody = baos.toString(StandardCharsets.UTF_8);
                            LlmResponse response = objectMapper.readValue(responseBody, LlmResponse.class);
                            return response != null ? response.getResponse() : null;
                        }
                    });

        } catch (Exception e) {
            log.error("Erreur lors de l'appel LLM (url: {}, model: {}): {} - {}", url, model,
                    e.getClass().getSimpleName(), e.getMessage());
        }
        return null; // Return null on error or disabled
    }
}

package fr.gymgod.etl.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gymgod.common.entities.nutrition.Glucide;
import fr.gymgod.common.entities.nutrition.Nutriment;
import fr.gymgod.common.entities.nutrition.Vitamin;
import fr.gymgod.etl.domain.model.OffProductData;
import fr.gymgod.etl.domain.port.OffProductFetchPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Récupération en direct d'un produit auprès d'OpenFoodFacts
 * ({@code GET /api/v2/product/{code}.json}) pour un code-barres scanné
 * absent de la base locale.
 *
 * <p>Throttlé à {@code off.nutrition.min-interval-ms} entre deux appels,
 * comme {@link OffNutritionEnrichmentGateway} — même API, même protection
 * anti-bot à respecter.
 */
@Service
@Slf4j
public class OffProductFetchGateway implements OffProductFetchPort {

    private static final String FIELDS = "product_name,quantity,ingredients_text,nutriscore_score,nutriscore_grade,nutriments,status";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final long minIntervalMs;

    private long lastCallTimeMs = 0;

    public OffProductFetchGateway(
            @Value("${off.nutrition.api-url:https://world.openfoodfacts.org/api/v2/product}") String apiUrl,
            @Value("${off.nutrition.user-agent:GymGod-App - https://github.com/Xafree/App-mobile-recipe-book-back}") String userAgent,
            @Value("${off.nutrition.enabled:true}") boolean enabled,
            @Value("${off.nutrition.min-interval-ms:1100}") long minIntervalMs,
            @Value("${off.nutrition.timeout-ms:5000}") int timeoutMs,
            ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.minIntervalMs = minIntervalMs;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("User-Agent", userAgent)
                .requestFactory(factory)
                .build();
    }

    @Override
    public synchronized Optional<OffProductData> fetchProduct(String code) {
        if (!enabled || code == null || code.isBlank()) {
            return Optional.empty();
        }

        throttle();

        try {
            String body = restClient.get()
                    .uri("/{code}.json?fields=" + FIELDS, code)
                    .retrieve()
                    .body(String.class);

            if (body == null) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(body);
            if (root.path("status").asInt(0) != 1) {
                log.debug("OFF: produit {} introuvable lors du scan (status != 1)", code);
                return Optional.empty();
            }

            JsonNode product = root.path("product");
            JsonNode nutriments = product.path("nutriments");

            return Optional.of(new OffProductData(
                    product.path("product_name").asText(null),
                    product.path("quantity").asText(null),
                    product.path("ingredients_text").asText(null),
                    product.path("nutriscore_score").asInt(0),
                    product.path("nutriscore_grade").asText(null),
                    toNutriment(nutriments),
                    toGlucide(nutriments),
                    toVitamin(nutriments)));
        } catch (Exception e) {
            log.warn("OFF: échec de la récupération du produit {} lors du scan: {}", code, e.getMessage());
            return Optional.empty();
        }
    }

    private Nutriment toNutriment(JsonNode n) {
        Nutriment nutriment = new Nutriment();
        nutriment.setEnergyKcal100g(field(n, "energy-kcal_100g"));
        nutriment.setProteins100g(field(n, "proteins_100g"));
        nutriment.setCarbohydrates100g(field(n, "carbohydrates_100g"));
        nutriment.setFat100g(field(n, "fat_100g"));
        nutriment.setFiber100g(field(n, "fiber_100g"));
        nutriment.setSugars100g(field(n, "sugars_100g"));
        nutriment.setSaturatedFat100g(field(n, "saturated-fat_100g"));
        return nutriment;
    }

    private Glucide toGlucide(JsonNode n) {
        Glucide glucide = new Glucide();
        glucide.setSodium100g(field(n, "sodium_100g"));
        glucide.setSalt100g(field(n, "salt_100g"));
        glucide.setPotassium100g(field(n, "potassium_100g"));
        glucide.setMagnesium100g(field(n, "magnesium_100g"));
        glucide.setCalcium100g(field(n, "calcium_100g"));
        return glucide;
    }

    private Vitamin toVitamin(JsonNode n) {
        Vitamin vitamin = new Vitamin();
        vitamin.setA100g(field(n, "vitamin-a_100g"));
        vitamin.setD100g(field(n, "vitamin-d_100g"));
        vitamin.setE100g(field(n, "vitamin-e_100g"));
        vitamin.setK100g(field(n, "vitamin-k_100g"));
        vitamin.setC100g(field(n, "vitamin-c_100g"));
        vitamin.setB1100g(field(n, "vitamin-b1_100g"));
        vitamin.setB2100g(field(n, "vitamin-b2_100g"));
        vitamin.setPp100g(field(n, "vitamin-pp_100g"));
        vitamin.setB6100g(field(n, "vitamin-b6_100g"));
        vitamin.setB9100g(field(n, "vitamin-b9_100g"));
        vitamin.setB12100g(field(n, "vitamin-b12_100g"));
        return vitamin;
    }

    private double field(JsonNode nutriments, String key) {
        return nutriments.path(key).asDouble(0);
    }

    /**
     * Bloque jusqu'à ce que {@code minIntervalMs} se soit écoulé depuis le
     * dernier appel — la méthode appelante est {@code synchronized} donc cette
     * limite s'applique à toutes les requêtes confondues.
     */
    private void throttle() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastCallTimeMs;
        if (elapsed < minIntervalMs) {
            try {
                Thread.sleep(minIntervalMs - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallTimeMs = System.currentTimeMillis();
    }
}

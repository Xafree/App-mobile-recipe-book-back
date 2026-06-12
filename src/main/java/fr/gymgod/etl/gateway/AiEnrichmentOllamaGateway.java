package fr.gymgod.etl.gateway;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gymgod.etl.domain.model.AdditiveData;
import fr.gymgod.etl.domain.port.AiEnrichmentPort;
import fr.gymgod.etl.gateway.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiEnrichmentOllamaGateway implements AiEnrichmentPort {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    // JSON Keys
    private static final String KEY_INGREDIENTS = "ingredients";
    private static final String KEY_ADDITIVES = "additives";
    private static final String KEY_ADDITIFS = "additifs"; // Fallback
    private static final String KEY_NOM = "nom";
    private static final String KEY_CODE_E = "code_e";
    private static final String KEY_RESPONSE = "response";

    // Configuration
    private static final double TEMPERATURE_ZERO = 0.0;
    private static final String TYPE_OBJECT = "object";
    private static final String TYPE_ARRAY = "array";
    private static final String TYPE_STRING = "string";

    // Prompt templates
    private static final String PROMPT_CLEAN_INGREDIENTS_TEMPLATE = """
            Tu es un expert en nutrition et un traducteur professionnel spécialisé dans l'agroalimentaire.
            Voici un texte brut (OCR) contenant une liste d'ingrédients :
            [%s]

            TACHE :
            1. Extrais la liste complète des ingrédients et traduis-les TOUS en FRANÇAIS standard. Aucun mot ne doit rester dans la langue d'origine.
            2. Utilise le vocabulaire officiel (ex: "Säuerungsmittel" -> "Acidifiant", "Äpfelsäure" -> "Acide malique", "Koffein" -> "Caféine").
            3. Si un ingrédient contient des sous-ingrédients entre parenthèses, extrais-les individuellement de manière claire.
            4. Inclus tout : aliments, additifs, vitamines, minéraux, conservateurs.
            5. Ignore le bruit (dates, adresses, avertissements de santé, valeurs nutritionnelles).
            6. Corrige les fautes de frappe liées à l'OCR.
            """;

    private static final String PROMPT_EXTRACT_ADDITIVES_TEMPLATE = """
            Tu es un expert en sécurité alimentaire.
            Voici une liste d'ingrédients en français :
            [%s]

            TACHE :
            1. Identifie TOUS les additifs alimentaires présents dans cette liste.
            2. Associe le Code E OFFICIEL et EXACT (ex: E330) à chaque additif.

            ATTENTION AUX CAS PARTICULIERS (NE LES OUBLIE PAS) :
            - Les vitamines agissant comme antioxydants/colorants SONT des additifs (ex: Acide ascorbique/Vitamine C = E300, Riboflavine = E101).
            - Les anti-agglomérants SONT des additifs (ex: Dioxyde de silicium / Oxyde de silicium = E551).
            - Les édulcorants SONT des additifs (ex: Sucralose = E955, Acesulfame K = E950).
            - Les correcteurs d'acidité SONT des additifs (ex: Acide malique = E296, Acide citrique = E330).

            CONTRAINTES :
            - Ne génère aucun texte d'introduction.
            - Renvoie UNIQUEMENT un objet JSON valide respectant ce format :
            {
              "additives": [
                {
                  "nom": "Nom de l'additif en français",
                  "code_e": "E..."
                }
              ]
            }
            """;

    @Override
    public CompletableFuture<List<String>> cleanIngredients(String rawInput) {
        return CompletableFuture.supplyAsync(() -> {
            // Define Schema
            Map<String, Object> jsonSchema = Map.of(
                    "type", TYPE_OBJECT,
                    "properties", Map.of(
                            KEY_INGREDIENTS, Map.of(
                                    "type", TYPE_ARRAY,
                                    "items", Map.of("type", TYPE_STRING))),
                    "required", List.of(KEY_INGREDIENTS));

            String prompt = String.format(PROMPT_CLEAN_INGREDIENTS_TEMPLATE, rawInput);

            return invokeLlmAndParse(prompt, jsonSchema, root -> {

                // Parsing logic specific to ingredients
                if (root.isArray()) {
                    List<IngredientData> list = convertValue(root, new TypeReference<>() {
                    });
                    return list.stream().map(IngredientData::getName).toList();
                }

                // 1. Check specific keys
                JsonNode targetNode = null;
                if (root.has(KEY_INGREDIENTS))
                    targetNode = root.get(KEY_INGREDIENTS);
                else if (root.has("ingrédients"))
                    targetNode = root.get("ingrédients");
                else if (root.has("Ingrédients"))
                    targetNode = root.get("Ingrédients");
                else if (root.has("ingredients_list"))
                    targetNode = root.get("ingredients_list");

                if (targetNode != null) {
                    return parseStringOrObjectList(targetNode);
                }

                // 2. Fallback: Search for ANY array property
                java.util.Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    if (field.getValue().isArray()) {
                        log.debug("Found potential ingredient array in field: {}", field.getKey());
                        List<String> result = parseStringOrObjectList(field.getValue());
                        if (!result.isEmpty())
                            return result;
                    }
                }

                if (root.has(KEY_RESPONSE)) {
                    // Fallback to split string
                    String response = root.get(KEY_RESPONSE).asText();
                    return splitIngredients(response);
                }

                String debugInput = rawInput.length() > 100 ? rawInput.substring(0, 100) + "..." : rawInput;
                String errorMsg = "Could not find ingredients list in LLM response: " + root.toString()
                        + " | Input was: [" + debugInput + "]";
                log.warn(errorMsg);
                throw new RuntimeException(errorMsg);
            }, Collections.emptyList());
        });
    }

    private List<String> parseStringOrObjectList(JsonNode node) {
        try {
            // Try simple string list
            return convertValue(node, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            // Fallback to object list
            try {
                List<IngredientData> list = convertValue(node, new TypeReference<List<IngredientData>>() {
                });
                return list.stream().map(IngredientData::getName).toList();
            } catch (Exception ex) {
                return Collections.emptyList();
            }
        }
    }

    private static class IngredientData {
        private String name;

        public String getName() {
            return name;
        }

        @SuppressWarnings("unused")
        public void setName(String name) {
            this.name = name;
        }
    }

    private List<String> splitIngredients(String input) {
        if (input == null || input.isBlank())
            return Collections.emptyList();

        List<String> result = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenthesisLevel = 0;

        for (char c : input.toCharArray()) {
            if (c == '(') {
                parenthesisLevel++;
            } else if (c == ')') {
                parenthesisLevel--;
            }

            if (c == ',' && parenthesisLevel == 0) {
                String trimmed = current.toString().trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        // Add the last part
        String trimmed = current.toString().trim();
        if (!trimmed.isEmpty()) {
            result.add(trimmed);
        }

        return result;
    }

    @Override
    public CompletableFuture<List<AdditiveData>> extractAdditives(List<String> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        String ingredientsListStr = String.join(", ", ingredients);
        String prompt = String.format(PROMPT_EXTRACT_ADDITIVES_TEMPLATE, ingredientsListStr);

        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> jsonSchema = Map.of(
                    "type", TYPE_OBJECT,
                    "properties", Map.of(
                            KEY_ADDITIVES, Map.of(
                                    "type", TYPE_ARRAY,
                                    "items", Map.of(
                                            "type", TYPE_OBJECT,
                                            "properties", Map.of(
                                                    KEY_NOM, Map.of("type", TYPE_STRING),
                                                    KEY_CODE_E, Map.of("type", TYPE_STRING)),
                                            "required", List.of(KEY_NOM, KEY_CODE_E)))),
                    "required", List.of(KEY_ADDITIVES));

            return invokeLlmAndParse(prompt, jsonSchema, root -> {

                JsonNode arrayNode = null;
                if (root.isArray()) {
                    arrayNode = root;
                } else {
                    // Search for first array field
                    if (root.has(KEY_ADDITIVES))
                        arrayNode = root.get(KEY_ADDITIVES);
                    else if (root.has(KEY_ADDITIFS))
                        arrayNode = root.get(KEY_ADDITIFS);
                    else if (root.has("additives_list"))
                        arrayNode = root.get("additives_list");

                    if (arrayNode == null) {
                        java.util.Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                        while (fields.hasNext()) {
                            Map.Entry<String, JsonNode> field = fields.next();
                            if (field.getValue().isArray()) {
                                log.debug("Found potential additive array in field: {}", field.getKey());
                                arrayNode = field.getValue();
                                break;
                            }
                        }
                    }
                }

                if (arrayNode != null) {
                    return convertValue(arrayNode, new TypeReference<List<AdditiveData>>() {
                    });
                }
                return Collections.emptyList();
            }, Collections.emptyList());
        });
    }

    private <T> T invokeLlmAndParse(String prompt, Map<String, Object> jsonSchema, Function<JsonNode, T> parser,
            T defaultValue) {
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", TEMPERATURE_ZERO);
        params.put("format", jsonSchema);
        params.put("stream", false);

        // Increase limits to avoid JsonEOFException (truncated response)
        Map<String, Object> options = new HashMap<>();
        options.put("num_ctx", 8192); // Increase context window
        options.put("num_predict", 4096); // Increase output limit
        params.put("options", options);

        String jsonResponse = llmClient.call(prompt, params);

        if (jsonResponse == null) {
            throw new RuntimeException("LLM API returned null response");
        }

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            return parser.apply(root);
        } catch (Exception e) {
            throw new RuntimeException("LLM API parsing failed: " + e.getMessage(), e);
        }
    }

    private <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) {
        return objectMapper.convertValue(fromValue, toValueTypeRef);
    }
}

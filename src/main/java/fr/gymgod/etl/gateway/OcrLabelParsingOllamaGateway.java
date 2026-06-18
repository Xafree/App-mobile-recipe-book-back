package fr.gymgod.etl.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gymgod.etl.domain.model.OcrNutritionData;
import fr.gymgod.etl.domain.port.OcrLabelParsingPort;
import fr.gymgod.etl.gateway.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Extraction à la demande des valeurs nutritionnelles (énergie, protéines,
 * glucides, lipides pour la portion imprimée, nom du produit) depuis le texte
 * brut OCR d'une étiquette nutritionnelle, via le LLM Ollama déjà configuré
 * ({@link LlmClient}).
 *
 * <p>Appel synchrone, ponctuel, déclenché par un scan utilisateur — pas de
 * throttle nécessaire (contrairement à {@code OffNutritionEnrichmentGateway}).
 *
 * <p>N'est plus l'implémentation par défaut ({@code @Primary} a été déplacé
 * sur {@link OcrLabelParsingPythonGateway}) — le flux de production n'appelle
 * plus de LLM. Conservée pour l'endpoint de comparaison
 * {@code /ocr/parse-label/compare}.
 */
@Service
@Qualifier("ocrOllama")
@RequiredArgsConstructor
@Slf4j
public class OcrLabelParsingOllamaGateway implements OcrLabelParsingPort {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    private static final double TEMPERATURE_ZERO = 0.0;
    private static final String TYPE_OBJECT = "object";
    private static final String TYPE_NUMBER = "number";

    private static final String KEY_SERVING_SIZE = "servingSizeG";
    private static final String KEY_ENERGY = "energyKcalServing";
    private static final String KEY_PROTEINS = "proteinsServing";
    private static final String KEY_CARBS = "carbohydratesServing";
    private static final String KEY_SUGARS = "sugarsServing";
    private static final String KEY_FAT = "fatServing";
    private static final String KEY_SATURATED_FAT = "saturatedFatServing";
    private static final String KEY_TRANS_FAT = "transFatServing";
    private static final String KEY_FIBER = "fiberServing";
    private static final String KEY_SODIUM = "sodiumMgServing";
    private static final String KEY_POTASSIUM = "potassiumMgServing";
    private static final String KEY_CALCIUM = "calciumMgServing";
    private static final String KEY_IRON = "ironMgServing";
    private static final String KEY_CHOLESTEROL = "cholesterolMgServing";

    private static final String KEY_PREPARED_SERVING_SIZE = "preparedServingSizeG";
    private static final String KEY_ENERGY_PREPARED = "energyKcalPreparedServing";
    private static final String KEY_PROTEINS_PREPARED = "proteinsPreparedServing";
    private static final String KEY_CARBS_PREPARED = "carbohydratesPreparedServing";
    private static final String KEY_SUGARS_PREPARED = "sugarsPreparedServing";
    private static final String KEY_FAT_PREPARED = "fatPreparedServing";
    private static final String KEY_SATURATED_FAT_PREPARED = "saturatedFatPreparedServing";
    private static final String KEY_TRANS_FAT_PREPARED = "transFatPreparedServing";
    private static final String KEY_FIBER_PREPARED = "fiberPreparedServing";
    private static final String KEY_SODIUM_PREPARED = "sodiumMgPreparedServing";

    private static final double DEFAULT_SERVING_SIZE_G = 100.0;

    private static final String PROMPT_PARSE_LABEL_TEMPLATE = """
            Tu es un expert en nutrition qui analyse le texte brut (OCR, potentiellement
            bruité, fautes de frappe, accents manquants, dans n'importe quelle langue)
            d'une étiquette nutritionnelle.

            EXEMPLE :
            Texte OCR :
            Nutrition Facts / Valeur nutritive
            Per 2/3 cup (55g) / pour 2/3 tasse (55g)
            Calories 230
            Fat / Lipides 4 g
            Saturated / saturés 1 g
            + Trans / trans 0 g
            Carbohydrate / Glucides 37 g
            Fibre / Fibres 4 g
            Sugars / Sucres 1 g
            Protein / Protéines 3 g
            Cholesterol / Cholestérol 5 mg
            Sodium 160 mg
            Potassium 250 mg
            Calcium 20 mg
            Iron / Fer 0.3 mg

            Réponse attendue :
            {"servingSizeG": 55, "energyKcalServing": 230,
             "proteinsServing": 3, "carbohydratesServing": 37, "sugarsServing": 1,
             "fatServing": 4, "saturatedFatServing": 1, "transFatServing": 0,
             "fiberServing": 4, "cholesterolMgServing": 5, "sodiumMgServing": 160,
             "potassiumMgServing": 250,
             "calciumMgServing": 20, "ironMgServing": 0.3,
             "preparedServingSizeG": null, "energyKcalPreparedServing": null,
             "proteinsPreparedServing": null, "carbohydratesPreparedServing": null,
             "sugarsPreparedServing": null, "fatPreparedServing": null,
             "saturatedFatPreparedServing": null, "transFatPreparedServing": null,
             "fiberPreparedServing": null, "sodiumMgPreparedServing": null}

            Note : aucune conversion vers 100 g n'a été faite — les valeurs sont
            celles imprimées pour la portion de 55 g (servingSizeG = 55). Cette
            étiquette n'a qu'une seule colonne de valeurs, donc tous les champs
            *PreparedServing sont null.

            EXEMPLE AVEC DEUX COLONNES (produit à préparer, ex. céréales
            déshydratées + lait, ou poudre à reconstituer) :
            Texte OCR :
            Valeurs nutritionnelles pour 100g / pour 250ml préparé (avec 200ml de
            lait demi-écrémé)
            Energie 380 kcal / 180 kcal
            Protéines 8 g / 7 g
            Glucides 70 g / 30 g
            dont sucres 25 g / 20 g
            Lipides 5 g / 4.5 g

            Réponse attendue (extrait) :
            {"servingSizeG": 100, "energyKcalServing": 380, "proteinsServing": 8,
             "carbohydratesServing": 70, "sugarsServing": 25, "fatServing": 5, ...,
             "preparedServingSizeG": 250, "energyKcalPreparedServing": 180,
             "proteinsPreparedServing": 7, "carbohydratesPreparedServing": 30,
             "sugarsPreparedServing": 20, "fatPreparedServing": 4.5, ...}

            EXEMPLE AVEC DEUX POIDS DANS LA PORTION MAIS UNE SEULE COLONNE DE
            VALEURS (le second poids décrit juste le résultat une fois préparé,
            ce n'est PAS une seconde colonne) :
            Texte OCR :
            Per 1/4 cup dry (28 g) / 140 g prepared
            pour 1/4 tasse sec (28g) / 140g préparé
            Calories 110
            Fat / Lipides 2.5 g
            Saturated / saturés 1.5 g
            + Trans / trans 0 g
            Carbohydrate / Glucides 20 g
            Fibre / Fibres 1 g
            Sugars / Sucres 1 g
            Protein / Protéines 2 g
            Sodium 450 mg

            Réponse attendue (extrait) :
            {"servingSizeG": 28, "energyKcalServing": 110, "proteinsServing": 2,
             "carbohydratesServing": 20, "sugarsServing": 1, "fatServing": 2.5, ...,
             "preparedServingSizeG": null, "energyKcalPreparedServing": null,
             "proteinsPreparedServing": null, "carbohydratesPreparedServing": null,
             "sugarsPreparedServing": null, "fatPreparedServing": null, ...}

            Note : "140 g prepared"/"140g préparé" ne fait que décrire le poids
            du produit une fois préparé pour CETTE portion de 28 g — il n'y a
            qu'UN SEUL jeu de valeurs nutritionnelles (Calories 110, Lipides
            2.5 g, etc.), donc preparedServingSizeG reste null.

            TEXTE OCR À ANALYSER :

            [%s]

            TACHE :
            1. Identifie la taille de la portion utilisée par le tableau de valeurs
               (ex : "Per 1/2 cup (125 g)", "pour 100 g", "par portion (30g)") et
               indique son poids/volume en grammes ou millilitres dans
               servingSizeG. Si le tableau indique déjà les valeurs "pour 100 g"
               ou "per 100g"/"100ml", utilise 100.
            2. Pour CETTE portion (PAS pour 100 g — n'effectue AUCUNE conversion),
               extrais les valeurs telles qu'imprimées :
               - énergie en kcal (si seule une valeur en kJ est présente, convertis-la
                 en kcal en divisant par 4.184 — c'est la SEULE conversion autorisée)
               - protéines en grammes
               - glucides totaux en grammes
               - dont sucres en grammes
               - lipides totaux en grammes
               - dont acides gras saturés en grammes
               - dont acides gras trans en grammes
               - fibres alimentaires en grammes
               - cholestérol en milligrammes
               - sodium en milligrammes (si seul le sel est indiqué, convertis-le
                 en sodium en multipliant par 1000 puis en divisant par 2.5)
               - potassium en milligrammes
               - calcium en milligrammes
               - fer en milligrammes
            3. Si une valeur est introuvable sur l'étiquette, renvoie null pour ce champ.
            4. Une SECONDE colonne de valeurs n'existe QUE si CHAQUE valeur
               nutritionnelle (énergie, protéines, glucides, lipides, etc.) est
               imprimée DEUX FOIS dans le tableau — une fois par colonne (ex.
               "Energie 380 kcal / 180 kcal", "Protéines 8 g / 7 g", ...). Dans
               ce cas uniquement, pour le produit "tel que
               préparé/cuit/reconstitué" (ex. "as prepared", "préparé", "cuit",
               "reconstitué", avec lait ajouté, etc.), répète les étapes 1 et 2
               pour cette seconde colonne dans preparedServingSizeG et les
               champs *PreparedServing correspondants (même règles de
               conversion kJ→kcal et sel→sodium).
               Sinon (l'étiquette n'a qu'UN SEUL jeu de valeurs
               nutritionnelles), laisse TOUS les champs *PreparedServing
               (incluant preparedServingSizeG) à null — y compris dans ces deux
               cas qui ne constituent JAMAIS une seconde colonne :
               - le mot "préparé"/"prepared" apparaît ailleurs sur l'étiquette
                 dans un contexte SANS RAPPORT avec la nutrition (ex. "Prepared
                 in Canada"/"Préparé au Canada" = pays de fabrication, "Garder
                 réfrigéré après ouverture", mentions légales, etc.) ;
               - la description de la portion mentionne DEUX poids/volumes
                 (ex. "Per 1/4 cup dry (28 g) / 140 g prepared", "pour 1/4
                 tasse sec (28g) 140g préparé") mais le tableau ne contient
                 qu'UN SEUL jeu de valeurs nutritionnelles : ce second poids ne
                 fait que décrire le résultat final et NE DOIT PAS être placé
                 dans preparedServingSizeG.

            CONTRAINTES :
            - Ne génère aucun texte d'introduction ni d'explication.
            - Renvoie UNIQUEMENT un objet JSON valide.
            """;

    @Override
    public Optional<OcrNutritionData> parseLabel(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(KEY_SERVING_SIZE, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_ENERGY, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_PROTEINS, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_CARBS, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_SUGARS, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_FAT, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_SATURATED_FAT, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_TRANS_FAT, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_FIBER, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_CHOLESTEROL, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_SODIUM, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_POTASSIUM, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_CALCIUM, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_IRON, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_PREPARED_SERVING_SIZE, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_ENERGY_PREPARED, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_PROTEINS_PREPARED, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_CARBS_PREPARED, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_SUGARS_PREPARED, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_FAT_PREPARED, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_SATURATED_FAT_PREPARED, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_TRANS_FAT_PREPARED, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_FIBER_PREPARED, Map.of("type", TYPE_NUMBER));
        properties.put(KEY_SODIUM_PREPARED, Map.of("type", TYPE_NUMBER));

        Map<String, Object> jsonSchema = Map.of(
                "type", TYPE_OBJECT,
                "properties", properties);

        String prompt = String.format(PROMPT_PARSE_LABEL_TEMPLATE, rawText);

        Map<String, Object> params = new HashMap<>();
        params.put("temperature", TEMPERATURE_ZERO);
        params.put("format", jsonSchema);
        params.put("stream", false);

        Map<String, Object> options = new HashMap<>();
        // num_ctx=4096 déclenche un crash CUDA (flash-attn MMA F16) sur certains
        // GPU (ex. RTX 5070 Ti) — 8192 est la valeur stable utilisée par
        // AiEnrichmentOllamaGateway, on s'aligne dessus.
        options.put("num_ctx", 8192);
        options.put("num_predict", 1024);
        params.put("options", options);

        try {
            String jsonResponse = llmClient.call(prompt, params);
            if (jsonResponse == null) {
                log.warn("OCR: le LLM n'a renvoyé aucune réponse pour l'étiquette.");
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(jsonResponse);

            // Aucune conversion vers 100 g/ml ici : les valeurs sont renvoyées
            // telles qu'imprimées pour servingSizeG, afin que l'utilisateur
            // puisse les comparer directement à l'étiquette physique. La
            // conversion vers 100 g/ml est faite côté client au moment de la
            // validation de l'ajout (voir _ManualPanel._confirm côté Flutter).
            Double servingSize = number(root, KEY_SERVING_SIZE);
            double resolvedServingSize = (servingSize == null || servingSize <= 0)
                    ? DEFAULT_SERVING_SIZE_G
                    : servingSize;

            return Optional.of(new OcrNutritionData(
                    resolvedServingSize,
                    number(root, KEY_ENERGY),
                    number(root, KEY_PROTEINS),
                    number(root, KEY_CARBS),
                    number(root, KEY_FAT),
                    number(root, KEY_SUGARS),
                    number(root, KEY_SATURATED_FAT),
                    number(root, KEY_TRANS_FAT),
                    number(root, KEY_FIBER),
                    number(root, KEY_CHOLESTEROL),
                    number(root, KEY_SODIUM),
                    number(root, KEY_POTASSIUM),
                    number(root, KEY_CALCIUM),
                    number(root, KEY_IRON),
                    number(root, KEY_PREPARED_SERVING_SIZE),
                    number(root, KEY_ENERGY_PREPARED),
                    number(root, KEY_PROTEINS_PREPARED),
                    number(root, KEY_CARBS_PREPARED),
                    number(root, KEY_FAT_PREPARED),
                    number(root, KEY_SUGARS_PREPARED),
                    number(root, KEY_SATURATED_FAT_PREPARED),
                    number(root, KEY_TRANS_FAT_PREPARED),
                    number(root, KEY_FIBER_PREPARED),
                    number(root, KEY_SODIUM_PREPARED)));
        } catch (Exception e) {
            log.warn("OCR: échec de l'extraction des valeurs nutritionnelles: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Double number(JsonNode root, String key) {
        JsonNode node = root.get(key);
        return (node == null || node.isNull()) ? null : node.asDouble();
    }
}

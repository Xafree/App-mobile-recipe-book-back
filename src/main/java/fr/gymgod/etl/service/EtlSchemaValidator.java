package fr.gymgod.etl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/// Vérifie que l'en-tête du fichier OFF place chaque colonne à l'index
/// attendu par {@link ProductMapper} (qui lit les colonnes par position, pas
/// par nom). Si OFF change son format d'export, cette vérification l'attrape
/// avant que l'ETL n'importe silencieusement des données décalées/erronées.
@Component
@Slf4j
public class EtlSchemaValidator {

    private static final Map<Integer, String> EXPECTED_COLUMNS = new LinkedHashMap<>();

    static {
        EXPECTED_COLUMNS.put(0, "code");
        EXPECTED_COLUMNS.put(1, "url");
        EXPECTED_COLUMNS.put(3, "created_t");
        EXPECTED_COLUMNS.put(5, "last_modified_t");
        EXPECTED_COLUMNS.put(10, "product_name");
        EXPECTED_COLUMNS.put(13, "quantity");
        EXPECTED_COLUMNS.put(18, "brands");
        EXPECTED_COLUMNS.put(21, "categories");
        EXPECTED_COLUMNS.put(29, "labels");
        EXPECTED_COLUMNS.put(39, "countries");
        EXPECTED_COLUMNS.put(42, "ingredients_text");
        EXPECTED_COLUMNS.put(47, "traces");
        EXPECTED_COLUMNS.put(57, "nutriscore_score");
        EXPECTED_COLUMNS.put(58, "nutriscore_grade");
        EXPECTED_COLUMNS.put(82, "image_url");
        EXPECTED_COLUMNS.put(84, "image_ingredients_url");
        EXPECTED_COLUMNS.put(86, "image_nutrition_url");
        EXPECTED_COLUMNS.put(89, "energy-kcal_100g");
        EXPECTED_COLUMNS.put(92, "fat_100g");
        EXPECTED_COLUMNS.put(93, "saturated-fat_100g");
        EXPECTED_COLUMNS.put(129, "carbohydrates_100g");
        EXPECTED_COLUMNS.put(130, "sugars_100g");
        EXPECTED_COLUMNS.put(146, "fiber_100g");
        EXPECTED_COLUMNS.put(150, "proteins_100g");
        EXPECTED_COLUMNS.put(154, "salt_100g");
        EXPECTED_COLUMNS.put(156, "sodium_100g");
        EXPECTED_COLUMNS.put(158, "vitamin-a_100g");
        EXPECTED_COLUMNS.put(160, "vitamin-d_100g");
        EXPECTED_COLUMNS.put(161, "vitamin-e_100g");
        EXPECTED_COLUMNS.put(162, "vitamin-k_100g");
        EXPECTED_COLUMNS.put(163, "vitamin-c_100g");
        EXPECTED_COLUMNS.put(164, "vitamin-b1_100g");
        EXPECTED_COLUMNS.put(165, "vitamin-b2_100g");
        EXPECTED_COLUMNS.put(166, "vitamin-pp_100g");
        EXPECTED_COLUMNS.put(167, "vitamin-b6_100g");
        EXPECTED_COLUMNS.put(168, "vitamin-b9_100g");
        EXPECTED_COLUMNS.put(170, "vitamin-b12_100g");
        EXPECTED_COLUMNS.put(175, "potassium_100g");
        EXPECTED_COLUMNS.put(177, "calcium_100g");
        EXPECTED_COLUMNS.put(180, "magnesium_100g");
    }

    /// @return true si toutes les colonnes attendues sont à leur index prévu dans {@code headerLine}.
    public boolean validate(String headerLine) {
        String[] columns = headerLine.split("\t", -1);
        boolean valid = true;

        for (Map.Entry<Integer, String> entry : EXPECTED_COLUMNS.entrySet()) {
            int idx = entry.getKey();
            String expectedName = entry.getValue();
            String actualName = idx < columns.length ? columns[idx] : null;

            if (!expectedName.equals(actualName)) {
                log.error("[Schéma] Colonne '{}' attendue à l'index {}, trouvée : '{}' — le format OFF a peut-être changé",
                        expectedName, idx, actualName);
                valid = false;
            }
        }

        if (valid) {
            log.info("[Schéma] {} colonnes clés vérifiées — format conforme à ProductMapper", EXPECTED_COLUMNS.size());
        }

        return valid;
    }
}

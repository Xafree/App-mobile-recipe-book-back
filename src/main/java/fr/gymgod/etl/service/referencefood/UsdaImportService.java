package fr.gymgod.etl.service.referencefood;

import fr.gymgod.common.entities.nutrition.ReferenceFood;
import fr.gymgod.common.entities.nutrition.ReferenceFoodSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Parse le jeu de données USDA FoodData Central "Foundation Foods"
 * (ZIP de CSV relationnels) en {@link ReferenceFood} (source
 * {@link ReferenceFoodSource#USDA}).
 */
@Service
@Slf4j
public class UsdaImportService {

    private static final String DATA_TYPE_FOUNDATION = "foundation_food";

    /** Identifiants de nutriments USDA standards utiles, vers le champ {@link ReferenceFood} correspondant. */
    private static final Map<Integer, BiConsumer<ReferenceFood, BigDecimal>> NUTRIENT_SETTERS = Map.ofEntries(
            Map.entry(1008, ReferenceFood::setCaloriesPer100g),
            Map.entry(1003, ReferenceFood::setProteinPer100g),
            Map.entry(1005, ReferenceFood::setCarbsPer100g),
            Map.entry(1004, ReferenceFood::setFatPer100g),
            Map.entry(1079, ReferenceFood::setFiberPer100g),
            Map.entry(2000, ReferenceFood::setSugarPer100g),
            Map.entry(1258, ReferenceFood::setSaturatedFatPer100g),
            Map.entry(1093, ReferenceFood::setSodiumMgPer100g),
            Map.entry(1092, ReferenceFood::setPotassiumMgPer100g),
            Map.entry(1087, ReferenceFood::setCalciumMgPer100g),
            Map.entry(1089, ReferenceFood::setIronMgPer100g));

    public List<ReferenceFood> parse(String zipFilePath) throws IOException {
        Map<String, byte[]> csvFiles = extractCsvFiles(zipFilePath,
                Set.of("food.csv", "food_nutrient.csv", "food_category.csv"));

        Map<String, String> categories = parseFoodCategories(csvFiles.get("food_category.csv"));
        List<ReferenceFood> foods = parseFoods(csvFiles.get("food.csv"), categories);

        Set<String> foundationIds = new HashSet<>();
        for (ReferenceFood food : foods) {
            foundationIds.add(food.getSourceCode());
        }
        applyNutrients(csvFiles.get("food_nutrient.csv"), foundationIds, foods);

        log.info("USDA Foundation Foods : {} aliments parsés depuis {}.", foods.size(), zipFilePath);
        return foods;
    }

    private Map<String, byte[]> extractCsvFiles(String zipFilePath, Set<String> wantedFileNames) throws IOException {
        Map<String, byte[]> result = new HashMap<>();
        try (FileInputStream fis = new FileInputStream(zipFilePath);
                ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName().contains("/")
                        ? entry.getName().substring(entry.getName().lastIndexOf('/') + 1)
                        : entry.getName();
                if (wantedFileNames.contains(fileName)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    zis.transferTo(out);
                    result.put(fileName, out.toByteArray());
                }
            }
        }
        for (String wanted : wantedFileNames) {
            if (!result.containsKey(wanted)) {
                throw new IOException("Fichier " + wanted + " introuvable dans l'archive USDA " + zipFilePath);
            }
        }
        return result;
    }

    private Map<String, String> parseFoodCategories(byte[] csvBytes) throws IOException {
        Map<String, String> categories = new HashMap<>();
        try (CSVParser parser = csvParser(csvBytes)) {
            for (CSVRecord record : parser) {
                categories.put(record.get("id"), record.get("description"));
            }
        }
        return categories;
    }

    private List<ReferenceFood> parseFoods(byte[] csvBytes, Map<String, String> categories) throws IOException {
        List<ReferenceFood> foods = new ArrayList<>();
        try (CSVParser parser = csvParser(csvBytes)) {
            for (CSVRecord record : parser) {
                if (!DATA_TYPE_FOUNDATION.equals(record.get("data_type"))) {
                    continue;
                }
                ReferenceFood food = new ReferenceFood();
                food.setSource(ReferenceFoodSource.USDA);
                food.setSourceCode(record.get("fdc_id"));
                food.setName(record.get("description"));
                food.setCategory(categories.get(record.get("food_category_id")));
                foods.add(food);
            }
        }
        return foods;
    }

    private void applyNutrients(byte[] csvBytes, Set<String> foundationIds, List<ReferenceFood> foods)
            throws IOException {
        Map<String, ReferenceFood> bySourceCode = new HashMap<>();
        for (ReferenceFood food : foods) {
            bySourceCode.put(food.getSourceCode(), food);
        }

        try (CSVParser parser = csvParser(csvBytes)) {
            for (CSVRecord record : parser) {
                String fdcId = record.get("fdc_id");
                if (!foundationIds.contains(fdcId)) {
                    continue;
                }

                int nutrientId;
                try {
                    nutrientId = Integer.parseInt(record.get("nutrient_id"));
                } catch (NumberFormatException e) {
                    continue;
                }

                BiConsumer<ReferenceFood, BigDecimal> setter = NUTRIENT_SETTERS.get(nutrientId);
                if (setter == null) {
                    continue;
                }

                BigDecimal amount = parseAmount(record.get("amount"));
                if (amount == null) {
                    continue;
                }

                setter.accept(bySourceCode.get(fdcId), amount);
            }
        }
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private CSVParser csvParser(byte[] csvBytes) throws IOException {
        return CSVParser.parse(
                new InputStreamReader(new ByteArrayInputStream(csvBytes), StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build());
    }
}

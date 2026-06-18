package fr.gymgod.etl.service.referencefood;

import fr.gymgod.common.entities.nutrition.ReferenceFood;
import fr.gymgod.common.entities.nutrition.ReferenceFoodSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class UsdaImportServiceTest {

    private static Path zipPath;

    private static final String FOOD_CSV = """
            "fdc_id","data_type","description","food_category_id","publication_date"
            "321358","foundation_food","Hummus, commercial","16","2019-04-01"
            "999999","sample_food","Should be ignored","16","2019-04-01"
            """;

    private static final String FOOD_CATEGORY_CSV = """
            "id","code","description"
            "16","1600","Legumes and Legume Products"
            """;

    private static final String FOOD_NUTRIENT_CSV = """
            "id","fdc_id","nutrient_id","amount","data_points","derivation_id","min","max","median","footnote","min_year_acquired"
            "1","321358","1008","229.0","","49","","","","",""
            "2","321358","1003","7.35","","49","","","","",""
            "3","321358","1005","14.9","","49","","","","",""
            "4","321358","1004","17.1","11","1","","","","",""
            "5","321358","1093","438.0","11","1","","","","",""
            "6","321358","1092","289.0","11","1","","","","",""
            "7","321358","1087","41.0","11","1","","","","",""
            "8","321358","1089","2.41","11","1","","","","",""
            "9","999999","1008","1.0","","49","","","","",""
            """;

    @BeforeAll
    static void writeFixture() throws IOException {
        zipPath = Files.createTempFile("usda_test", ".zip");

        try (OutputStream fos = Files.newOutputStream(zipPath);
                ZipOutputStream zos = new ZipOutputStream(fos)) {
            writeEntry(zos, "FoodData_Central_foundation_food_csv_2026-04-30/food.csv", FOOD_CSV);
            writeEntry(zos, "FoodData_Central_foundation_food_csv_2026-04-30/food_category.csv", FOOD_CATEGORY_CSV);
            writeEntry(zos, "FoodData_Central_foundation_food_csv_2026-04-30/food_nutrient.csv", FOOD_NUTRIENT_CSV);
        }
    }

    private static void writeEntry(ZipOutputStream zos, String name, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    @AfterAll
    static void cleanup() throws IOException {
        Files.deleteIfExists(zipPath);
    }

    @Test
    void parseExtraitUniquementLesFoundationFoodsAvecLeursValeurs() throws Exception {
        List<ReferenceFood> foods = new UsdaImportService().parse(zipPath.toString());

        assertThat(foods).hasSize(1);

        ReferenceFood hummus = foods.get(0);
        assertThat(hummus.getSource()).isEqualTo(ReferenceFoodSource.USDA);
        assertThat(hummus.getSourceCode()).isEqualTo("321358");
        assertThat(hummus.getName()).isEqualTo("Hummus, commercial");
        assertThat(hummus.getCategory()).isEqualTo("Legumes and Legume Products");
        assertThat(hummus.getCaloriesPer100g()).isEqualTo(new BigDecimal("229.0"));
        assertThat(hummus.getProteinPer100g()).isEqualTo(new BigDecimal("7.35"));
        assertThat(hummus.getCarbsPer100g()).isEqualTo(new BigDecimal("14.9"));
        assertThat(hummus.getFatPer100g()).isEqualTo(new BigDecimal("17.1"));
        assertThat(hummus.getSodiumMgPer100g()).isEqualTo(new BigDecimal("438.0"));
        assertThat(hummus.getPotassiumMgPer100g()).isEqualTo(new BigDecimal("289.0"));
        assertThat(hummus.getCalciumMgPer100g()).isEqualTo(new BigDecimal("41.0"));
        assertThat(hummus.getIronMgPer100g()).isEqualTo(new BigDecimal("2.41"));
        // Pas de valeur de sucre dans le fixture -> reste null.
        assertThat(hummus.getSugarPer100g()).isNull();
    }
}

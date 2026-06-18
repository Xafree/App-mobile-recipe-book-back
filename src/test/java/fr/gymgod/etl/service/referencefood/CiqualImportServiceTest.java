package fr.gymgod.etl.service.referencefood;

import fr.gymgod.common.entities.nutrition.ReferenceFood;
import fr.gymgod.common.entities.nutrition.ReferenceFoodSource;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CiqualImportServiceTest {

    private static Path xlsxPath;

    private static final String[] HEADERS = {
            "alim_grp_code", "alim_grp_nom_fr", "alim_code", "alim_nom_fr",
            "Energie,\nRèglement\nUE N°\n1169\n2011 (kcal\n100 g)",
            "Protéines,\nN x\nfacteur de\nJones (g\n100 g)",
            "Glucides\n(g\n100 g)",
            "Lipides\n(g\n100 g)",
            "Sucres\n(g\n100 g)",
            "Fibres\nalimentaires\n(g\n100 g)",
            "AG\nsaturés\n(g\n100 g)",
            "Sodium\n(mg\n100 g)",
            "Potassium\n(mg\n100 g)",
            "Calcium\n(mg\n100 g)",
            "Fer (mg\n100 g)"
    };

    @BeforeAll
    static void writeFixture() throws IOException {
        xlsxPath = Files.createTempFile("ciqual_test", ".xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Table Ciqual");

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }

            // Aliment "normal" avec valeurs décimales (virgule) et limite de quantification.
            Row banane = sheet.createRow(1);
            String[] bananeValues = { "09", "fruits", "13001", "Banane, pulpe, crue",
                    "89", "1,1", "20", "0,3", "12", "2,6", "0,1", "1", "360", "5", "< 0,3" };
            for (int i = 0; i < bananeValues.length; i++) {
                banane.createCell(i).setCellValue(bananeValues[i]);
            }

            // Aliment avec valeurs non déterminées ("-") et "traces".
            Row inconnu = sheet.createRow(2);
            String[] inconnuValues = { "09", "fruits", "13002", "Fruit obscur, cru",
                    "-", "traces", "-", "-", "-", "-", "-", "-", "-", "-", "-" };
            for (int i = 0; i < inconnuValues.length; i++) {
                inconnu.createCell(i).setCellValue(inconnuValues[i]);
            }

            try (FileOutputStream out = new FileOutputStream(xlsxPath.toFile())) {
                workbook.write(out);
            }
        }
    }

    @AfterAll
    static void cleanup() throws IOException {
        Files.deleteIfExists(xlsxPath);
    }

    @Test
    void parseExtraitLesAlimentsAvecLeursValeursPer100g() throws Exception {
        List<ReferenceFood> foods = new CiqualImportService().parse(xlsxPath.toString());

        assertThat(foods).hasSize(2);

        ReferenceFood banane = foods.get(0);
        assertThat(banane.getSource()).isEqualTo(ReferenceFoodSource.CIQUAL);
        assertThat(banane.getSourceCode()).isEqualTo("13001");
        assertThat(banane.getName()).isEqualTo("Banane, pulpe, crue");
        assertThat(banane.getCategory()).isEqualTo("fruits");
        assertThat(banane.getCaloriesPer100g()).isEqualTo(new BigDecimal("89"));
        assertThat(banane.getProteinPer100g()).isEqualTo(new BigDecimal("1.1"));
        assertThat(banane.getCarbsPer100g()).isEqualTo(new BigDecimal("20"));
        assertThat(banane.getFatPer100g()).isEqualTo(new BigDecimal("0.3"));
        assertThat(banane.getSugarPer100g()).isEqualTo(new BigDecimal("12"));
        assertThat(banane.getFiberPer100g()).isEqualTo(new BigDecimal("2.6"));
        assertThat(banane.getSaturatedFatPer100g()).isEqualTo(new BigDecimal("0.1"));
        assertThat(banane.getSodiumMgPer100g()).isEqualTo(new BigDecimal("1"));
        assertThat(banane.getPotassiumMgPer100g()).isEqualTo(new BigDecimal("360"));
        assertThat(banane.getCalciumMgPer100g()).isEqualTo(new BigDecimal("5"));
        // "< 0,3" -> estimation haute 0.3
        assertThat(banane.getIronMgPer100g()).isEqualTo(new BigDecimal("0.3"));
    }

    @Test
    void valeursNonDeterrmineesOuTracesSontGereesSansErreur() throws Exception {
        List<ReferenceFood> foods = new CiqualImportService().parse(xlsxPath.toString());

        ReferenceFood inconnu = foods.get(1);
        assertThat(inconnu.getSourceCode()).isEqualTo("13002");
        // "-" -> non déterminé
        assertThat(inconnu.getCaloriesPer100g()).isNull();
        assertThat(inconnu.getCarbsPer100g()).isNull();
        // "traces" -> 0
        assertThat(inconnu.getProteinPer100g()).isEqualTo(BigDecimal.ZERO);
    }
}

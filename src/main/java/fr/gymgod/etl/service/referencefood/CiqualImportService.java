package fr.gymgod.etl.service.referencefood;

import fr.gymgod.common.entities.nutrition.ReferenceFood;
import fr.gymgod.common.entities.nutrition.ReferenceFoodSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parse la table de composition nutritionnelle CIQUAL (fichier Excel "Table
 * Ciqual <année>_FR_*.xlsx", 1 ligne = 1 aliment, ~70 colonnes) en
 * {@link ReferenceFood} (source {@link ReferenceFoodSource#CIQUAL}).
 *
 * <p>Les colonnes sont retrouvées par leur libellé (ligne d'en-tête), normalisé
 * (sans accents, ponctuation ni saut de ligne) puis comparé par jeu de
 * tokens — robuste aux légères variations de mise en forme entre éditions.
 */
@Service
@Slf4j
public class CiqualImportService {

    public List<ReferenceFood> parse(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
                Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("Feuille CIQUAL vide : aucune ligne d'en-tête.");
            }

            Map<String, Integer> columns = buildColumnIndex(headerRow);
            List<ReferenceFood> foods = new ArrayList<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }

                String code = cellText(row, columns.get("code"));
                String name = cellText(row, columns.get("name"));
                if (code == null || code.isBlank() || name == null || name.isBlank()) {
                    continue;
                }

                ReferenceFood food = new ReferenceFood();
                food.setSource(ReferenceFoodSource.CIQUAL);
                food.setSourceCode(code.trim());
                food.setName(name.trim());
                food.setCategory(cellText(row, columns.get("category")));
                food.setCaloriesPer100g(parseValue(cellText(row, columns.get("calories"))));
                food.setProteinPer100g(parseValue(cellText(row, columns.get("protein"))));
                food.setCarbsPer100g(parseValue(cellText(row, columns.get("carbs"))));
                food.setFatPer100g(parseValue(cellText(row, columns.get("fat"))));
                food.setFiberPer100g(parseValue(cellText(row, columns.get("fiber"))));
                food.setSugarPer100g(parseValue(cellText(row, columns.get("sugar"))));
                food.setSaturatedFatPer100g(parseValue(cellText(row, columns.get("saturatedFat"))));
                food.setSodiumMgPer100g(parseValue(cellText(row, columns.get("sodium"))));
                food.setPotassiumMgPer100g(parseValue(cellText(row, columns.get("potassium"))));
                food.setCalciumMgPer100g(parseValue(cellText(row, columns.get("calcium"))));
                food.setIronMgPer100g(parseValue(cellText(row, columns.get("iron"))));

                foods.add(food);
            }

            log.info("CIQUAL : {} aliments parsés depuis {}.", foods.size(), filePath);
            return foods;
        }
    }

    /**
     * Associe chaque champ {@link ReferenceFood} à l'index de colonne CIQUAL
     * correspondant, en faisant correspondre les en-têtes par jeu de tokens
     * (insensible aux accents/sauts de ligne/casse).
     */
    private Map<String, Integer> buildColumnIndex(Row headerRow) {
        Map<String, Integer> columns = new HashMap<>();

        for (Cell cell : headerRow) {
            String raw = cellText(headerRow, cell.getColumnIndex());
            if (raw == null) {
                continue;
            }
            Set<String> tokens = tokens(raw);
            int col = cell.getColumnIndex();

            if (tokens.equals(Set.of("alim", "code"))) {
                columns.put("code", col);
            } else if (tokens.equals(Set.of("alim", "nom", "fr"))) {
                columns.put("name", col);
            } else if (tokens.equals(Set.of("alim", "grp", "nom", "fr"))) {
                columns.put("category", col);
            } else if (tokens.contains("energie") && tokens.contains("kcal") && !tokens.contains("jones")) {
                columns.put("calories", col);
            } else if (tokens.contains("proteines") && tokens.contains("jones")) {
                columns.put("protein", col);
            } else if (tokens.contains("glucides")) {
                columns.put("carbs", col);
            } else if (tokens.contains("lipides")) {
                columns.put("fat", col);
            } else if (tokens.contains("sucres")) {
                columns.put("sugar", col);
            } else if (tokens.contains("fibres") && tokens.contains("alimentaires")) {
                columns.put("fiber", col);
            } else if (tokens.contains("ag") && tokens.contains("satures")) {
                columns.put("saturatedFat", col);
            } else if (tokens.contains("sodium") && tokens.contains("mg")) {
                columns.put("sodium", col);
            } else if (tokens.contains("potassium")) {
                columns.put("potassium", col);
            } else if (tokens.contains("calcium")) {
                columns.put("calcium", col);
            } else if (tokens.contains("fer") && tokens.contains("mg")) {
                columns.put("iron", col);
            }
        }

        for (String required : List.of("code", "name", "calories", "protein", "carbs", "fat")) {
            if (!columns.containsKey(required)) {
                log.warn("Colonne CIQUAL '{}' introuvable dans l'en-tête — champ laissé vide.", required);
            }
        }

        return columns;
    }

    /** Normalise un libellé d'en-tête : sans accents/ponctuation, minuscules, espaces simples. */
    private Set<String> tokens(String header) {
        String noAccents = Normalizer.normalize(header, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String cleaned = noAccents.replaceAll("[^a-zA-Z0-9]+", " ").toLowerCase().trim();
        if (cleaned.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(List.of(cleaned.split("\\s+")));
    }

    private String cellText(Row row, Integer colIndex) {
        if (colIndex == null) {
            return null;
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BLANK -> null;
            default -> cell.toString();
        };
    }

    /**
     * Convertit une valeur CIQUAL en {@link BigDecimal} :
     * <ul>
     *   <li>{@code -} ou vide → {@code null} (non déterminé)</li>
     *   <li>{@code traces} → {@code 0}</li>
     *   <li>{@code <x} (limite de quantification) → {@code x}, pris comme estimation haute</li>
     *   <li>virgule décimale → point</li>
     * </ul>
     */
    BigDecimal parseValue(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty() || value.equals("-")) {
            return null;
        }
        if (value.equalsIgnoreCase("traces")) {
            return BigDecimal.ZERO;
        }
        if (value.startsWith("<")) {
            value = value.substring(1).trim();
        }
        value = value.replace(',', '.');
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.debug("Valeur CIQUAL non numérique ignorée : '{}'", raw);
            return null;
        }
    }
}

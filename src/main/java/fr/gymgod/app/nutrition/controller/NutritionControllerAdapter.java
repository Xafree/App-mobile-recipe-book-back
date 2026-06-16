package fr.gymgod.app.nutrition.controller;

import fr.gymgod.app.nutrition.domain.entites.record.OcrLabelCompareResult;
import fr.gymgod.app.nutrition.domain.entites.record.OcrLabelParseRequest;
import fr.gymgod.app.nutrition.domain.entites.record.OcrNutritionRecord;
import fr.gymgod.app.nutrition.domain.entites.record.ProductRecord;
import fr.gymgod.app.nutrition.service.OcrLabelComparisonService;
import fr.gymgod.app.nutrition.service.OrchestratorNutrition;
import fr.gymgod.common.constants.ConstantsCommon;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping(ConstantsCommon.ENDPOINT_NUTRITION)
@RequiredArgsConstructor
public class NutritionControllerAdapter {

    private final OrchestratorNutrition orchestratorNutrition;
    private final OcrLabelComparisonService ocrLabelComparisonService;

    @Value("${path.image}")
    private String NAME_FOLDER;

    @GetMapping("/{key}")
    public ResponseEntity<ProductRecord> get(@PathVariable String key) {
        System.out.println("Recherche du produit : " + key);
        ProductRecord product = this.orchestratorNutrition.getProduct(key);
        System.out.println("return product: " + product);
        return ResponseEntity.ok(product);
    }

    @PostMapping("/ocr/parse-label")
    public ResponseEntity<OcrNutritionRecord> parseLabel(@RequestBody OcrLabelParseRequest request) {
        System.out.println("return record: " + request);
        OcrNutritionRecord record = this.orchestratorNutrition.parseOcrLabel(request.rawText());
        System.out.println("return record: " + record);
        return ResponseEntity.ok(record);
    }

    /**
     * Exécute le LLM Ollama et le parseur regex sur le même texte OCR et
     * renvoie les deux résultats côte à côte, pour comparaison manuelle.
     * N'affecte pas {@code /ocr/parse-label} (flux de production inchangé).
     */
    @PostMapping("/ocr/parse-label/compare")
    public ResponseEntity<OcrLabelCompareResult> compareLabel(@RequestBody OcrLabelParseRequest request) {
        return ResponseEntity.ok(this.ocrLabelComparisonService.compare(request.rawText()));
    }

    /**
     * Résultat du seul gateway Python (ocr-label-parser), au même format que
     * {@code /ocr/parse-label} (LLM) — pour tester ce parseur depuis
     * l'application mobile (cf. {@code --dart-define=OCR_PARSER_VARIANT=python}).
     * N'affecte pas {@code /ocr/parse-label} (flux de production inchangé).
     */
    @PostMapping("/ocr/parse-label/python")
    public ResponseEntity<OcrNutritionRecord> parseLabelPython(@RequestBody OcrLabelParseRequest request) {
        //System.out.println("Parsing label with Python: " + request.rawText());
        return ResponseEntity.ok(this.ocrLabelComparisonService.python(request.rawText()));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductRecord>> searchFoods(@RequestParam String name,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(orchestratorNutrition.searchProducts(name, page, size));
    }

    @GetMapping("/image/{code}/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String code, @PathVariable String filename) {
        System.out.println("Recherche de l'image : " + code + "/" + filename);
        try {
            Path file = Paths.get(NAME_FOLDER).resolve(code).resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/ingredients/cleanup")
    public ResponseEntity<String> cleanupIngredients(@RequestParam(defaultValue = "1000") int limit) {
        int count = orchestratorNutrition.cleanupOrphanIngredients(limit);
        return ResponseEntity.ok("Nombre d'ingrédients supprimés : " + count);
    }

    @PostMapping("/admin/maintenance/deduplicate-ingredients")
    public ResponseEntity<String> deduplicateIngredients() {
        try {
            String result = orchestratorNutrition.deduplicateIngredients();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error during deduplication: " + e.getMessage());
        }
    }
}

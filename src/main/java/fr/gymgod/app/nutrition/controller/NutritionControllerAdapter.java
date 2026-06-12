package fr.gymgod.app.nutrition.controller;

import fr.gymgod.app.nutrition.domain.entites.record.ProductRecord;
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

    @Value("${path.image}")
    private String NAME_FOLDER;

    @GetMapping("/{key}")
    public ResponseEntity<ProductRecord> get(@PathVariable String key) {
        System.out.println("Recherche du produit : " + key);
        ProductRecord product = this.orchestratorNutrition.getProduct(key);
        return ResponseEntity.ok(product);
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

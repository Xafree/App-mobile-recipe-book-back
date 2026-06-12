package fr.gymgod.app.recipe.controller;

import fr.gymgod.app.recipe.domain.entites.record.RecipeCreateRecord;
import fr.gymgod.app.recipe.domain.entites.record.RecipeRecord;
import fr.gymgod.app.recipe.service.OrchestratorRecipe;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeControllerAdapter {

    private final OrchestratorRecipe orchestratorRecipe;

    @Value("${path.image}")
    private String imageFolder;

    @GetMapping
    public ResponseEntity<List<RecipeRecord>> getUserRecipes() {
        return ResponseEntity.ok(orchestratorRecipe.getUserRecipes());
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<RecipeRecord>> getUserFavoriteRecipes() {
        return ResponseEntity.ok(orchestratorRecipe.getUserFavoriteRecipes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeRecord> getRecipe(@PathVariable UUID id) {
        RecipeRecord recipe = orchestratorRecipe.getRecipe(id);
        if (recipe != null) {
            return ResponseEntity.ok(recipe);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<RecipeRecord> createRecipe(@RequestBody @Valid RecipeCreateRecord request) {
        RecipeRecord created = orchestratorRecipe.createRecipe(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecipeRecord> updateRecipe(@PathVariable UUID id,
            @RequestBody @Valid RecipeCreateRecord request) {
        RecipeRecord updated = orchestratorRecipe.updateRecipe(id, request);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable UUID id) {
        orchestratorRecipe.deleteRecipe(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Upload d'image pour une recette.
     * Sauvegarde le fichier dans {@code {path.image}/recipes/} et retourne l'URL relative.
     * Exemple de réponse : {@code { "url": "/api/v1/recipes/image/uuid.jpg" }}
     */
    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fichier vide"));
        }
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.'))
                : ".jpg";
        String filename = UUID.randomUUID() + ext;

        Path dir = Paths.get(imageFolder, "recipes");
        Files.createDirectories(dir);
        Files.write(dir.resolve(filename), file.getBytes());

        return ResponseEntity.ok(Map.of("url", "/api/v1/recipes/image/" + filename));
    }

    /**
     * Sert une image de recette depuis le disque.
     * Accessible publiquement (GET, voir SecurityConfig).
     */
    @GetMapping("/image/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            Path file = Paths.get(imageFolder, "recipes").resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

package fr.gymgod.app.ingredient.controller;

import fr.gymgod.app.ingredient.domain.entites.record.CatalogIngredientCreateRecord;
import fr.gymgod.app.ingredient.domain.entites.record.CatalogIngredientRecord;
import fr.gymgod.app.ingredient.service.OrchestratorCatalogIngredient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catalogue d'ingrédients partagé — sauvegarde (saisie manuelle ou OCR) et
 * réutilisation par tous les utilisateurs sans ressaisie.
 */
@RestController
@RequestMapping("/api/v1/ingredients")
@RequiredArgsConstructor
public class CatalogIngredientControllerAdapter {

    private final OrchestratorCatalogIngredient orchestratorCatalogIngredient;

    @PostMapping
    public ResponseEntity<CatalogIngredientRecord> save(@RequestBody @Valid CatalogIngredientCreateRecord request) {
        CatalogIngredientRecord saved = orchestratorCatalogIngredient.save(request);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    /**
     * Recherche paginée (50 résultats max) du catalogue partagé par nom —
     * {@code name} absent ou vide renvoie les premiers ingrédients du
     * catalogue. Même contrat que le catalogue Open Food Facts côté front
     * (recherche debouncée).
     */
    @GetMapping
    public ResponseEntity<List<CatalogIngredientRecord>> getCatalogIngredients(
            @RequestParam(required = false, defaultValue = "") String name) {
        return ResponseEntity.ok(orchestratorCatalogIngredient.getCatalogIngredients(name));
    }

    /**
     * Recherche un ingrédient du catalogue partagé par code-barres — flux
     * re-scan : retrouve un ingrédient saisi/scanné par n'importe quel
     * utilisateur. Retourne {@code 404} si aucun ingrédient n'est lié à ce
     * code-barres.
     */
    @GetMapping("/by-code/{code}")
    public ResponseEntity<CatalogIngredientRecord> getByCode(@PathVariable String code) {
        return orchestratorCatalogIngredient.getByExternalFoodCode(code)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

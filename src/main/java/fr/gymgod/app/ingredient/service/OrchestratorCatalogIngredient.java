package fr.gymgod.app.ingredient.service;

import fr.gymgod.app.ingredient.domain.entites.record.CatalogIngredientCreateRecord;
import fr.gymgod.app.ingredient.domain.entites.record.CatalogIngredientRecord;
import fr.gymgod.app.ingredient.domain.mapper.CatalogIngredientTransform;
import fr.gymgod.app.ingredient.domain.port.CatalogIngredientDataPort;
import fr.gymgod.common.entities.nutrition.CatalogIngredient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrchestratorCatalogIngredient {

    /** Nombre maximum d'ingrédients renvoyés par {@link #getCatalogIngredients}. */
    private static final int SEARCH_LIMIT = 50;

    private final CatalogIngredientDataPort catalogIngredientDataPort;
    private final CatalogIngredientTransform catalogIngredientTransform;

    /**
     * Sauvegarde un ingrédient du catalogue partagé — upsert par nom
     * insensible à la casse : un nouveau scan/saisie du même ingrédient met
     * à jour les valeurs existantes plutôt que de créer un doublon. Visible
     * et réutilisable par tous les utilisateurs.
     */
    @Transactional
    public CatalogIngredientRecord save(CatalogIngredientCreateRecord request) {
        CatalogIngredient ingredient = catalogIngredientDataPort
                .findByName(request.name())
                .orElseGet(CatalogIngredient::new);
        catalogIngredientTransform.applyCreateRecord(ingredient, request);
        return catalogIngredientTransform.fromEntity(catalogIngredientDataPort.save(ingredient));
    }

    /**
     * Recherche paginée du catalogue partagé par nom, triée par nom —
     * {@code name} vide renvoie les premiers ingrédients du catalogue.
     * Limite les résultats à {@link #SEARCH_LIMIT} pour rester performant
     * quel que soit le nombre d'utilisateurs ayant alimenté le catalogue.
     */
    public List<CatalogIngredientRecord> getCatalogIngredients(String name) {
        return catalogIngredientTransform.fromEntities(
                catalogIngredientDataPort.search(name == null ? "" : name, SEARCH_LIMIT));
    }

    /**
     * Recherche un ingrédient du catalogue partagé par code-barres — permet
     * de retrouver un ingrédient saisi/scanné par un autre utilisateur en
     * re-scannant le même code-barres.
     */
    public Optional<CatalogIngredientRecord> getByExternalFoodCode(String externalFoodCode) {
        return catalogIngredientDataPort.findByExternalFoodCode(externalFoodCode)
                .map(catalogIngredientTransform::fromEntity);
    }
}

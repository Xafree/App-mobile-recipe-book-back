package fr.gymgod.app.ingredient.domain.port;

import fr.gymgod.common.entities.nutrition.CatalogIngredient;

import java.util.List;
import java.util.Optional;

public interface CatalogIngredientDataPort {
    Optional<CatalogIngredient> findByName(String name);

    Optional<CatalogIngredient> findByExternalFoodCode(String externalFoodCode);

    /**
     * Recherche paginée du catalogue partagé par nom (insensible à la
     * casse), triée par nom — {@code name} vide renvoie les premiers
     * ingrédients du catalogue. {@code limit} borne le nombre de résultats.
     */
    List<CatalogIngredient> search(String name, int limit);

    CatalogIngredient save(CatalogIngredient ingredient);
}

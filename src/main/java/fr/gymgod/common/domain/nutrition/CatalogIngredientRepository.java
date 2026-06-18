package fr.gymgod.common.domain.nutrition;

import fr.gymgod.common.entities.nutrition.CatalogIngredient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CatalogIngredientRepository extends JpaRepository<CatalogIngredient, UUID> {
    Optional<CatalogIngredient> findByNameIgnoreCase(String name);

    /**
     * Recherche paginée par nom (insensible à la casse), triée par nom —
     * {@code name} vide (avec {@code Pageable}) renvoie les premiers
     * ingrédients du catalogue. Évite de charger l'intégralité du catalogue
     * partagé à chaque appel, quel que soit le nombre d'utilisateurs.
     */
    List<CatalogIngredient> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);

    Optional<CatalogIngredient> findByExternalFoodCode(String externalFoodCode);
}

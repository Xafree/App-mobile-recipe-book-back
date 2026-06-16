package fr.gymgod.common.domain.nutrition;

import fr.gymgod.common.entities.nutrition.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    // Limite à 20 résultats pour la performance
    List<Product> findTop20ByProductNameContainingIgnoreCase(String productName);

    // Conservé pour les appelants qui ont besoin du total (COUNT) — ex. admin.
    org.springframework.data.domain.Page<Product> findByProductNameContainingIgnoreCase(String productName, org.springframework.data.domain.Pageable pageable);

    /**
     * Recherche paginée pour le picker d'ingrédients — <strong>sans COUNT query</strong>.
     *
     * <p>Retourner {@code List<T>} avec un paramètre {@code Pageable} applique
     * LIMIT/OFFSET au niveau SQL sans déclencher la seconde requête {@code COUNT(*)}
     * que génère {@code Page<T>}. Sur 656 k produits, cette requête de comptage
     * peut prendre plusieurs centaines de millisecondes — l'éliminer divise par deux
     * le temps de réponse pour la recherche par nom.
     *
     * <p>Si {@code name} est vide, la clause {@code WHERE} est court-circuitée
     * ({@code :name = ''}) pour que PostgreSQL puisse utiliser un simple
     * {@code LIMIT 50} sans évaluer le prédicat {@code ILIKE} sur chaque ligne.
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT p FROM Product p " +
            "WHERE :name = '' OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :name, '%'))"
    )
    List<Product> findByNameForPicker(
            @org.springframework.data.repository.query.Param("name") String name,
            org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.aiEnriched = false AND p.ingredientsText IS NOT NULL AND p.ingredientsText <> ''")
    List<Product> findTop50ByAiEnrichedFalseAndIngredientsTextIsNotNull(org.springframework.data.domain.Pageable pageable);

    /**
     * Produits bruts/génériques (sans marque) dont les macros sont
     * manquantes et qui n'ont pas encore été soumis au job hebdomadaire
     * {@code ReferenceFoodEnrichmentJobAdapter} (correspondance CIQUAL/USDA).
     */
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.nutritionDataIncomplete = true " +
            "AND p.referenceEnrichmentAttempted = false AND p.brand IS NULL")
    List<Product> findPendingReferenceEnrichment(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.image IS NOT NULL AND (" +
            "(p.image.imageUrl IS NOT NULL AND p.image.isImageUrlDownload = false) OR " +
            "(p.image.imageIngredientUrl IS NOT NULL AND p.image.isImageIngredientUrlDownload = false) OR " +
            "(p.image.imageNutritionUrl IS NOT NULL AND p.image.isImageNutritionUrlDownload = false))")
    List<Product> findProductsWithPendingImages();
}

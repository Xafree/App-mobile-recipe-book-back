package fr.gymgod.etl.domain.port;

import fr.gymgod.etl.domain.model.NutritionEnrichmentData;

import java.util.Optional;

/**
 * Port pour la récupération à la demande des macros/minéraux/vitamines d'un
 * produit auprès d'OpenFoodFacts, quand ces données sont absentes de l'export
 * CSV (cf. {@code Product.nutritionDataIncomplete}).
 */
public interface NutritionEnrichmentPort {

    /**
     * @param code code-barres OFF du produit (Product.code)
     * @return les données nutritionnelles si le produit a été trouvé sur OFF,
     *         {@link Optional#empty()} si la requête a échoué ou si le produit
     *         est introuvable.
     */
    Optional<NutritionEnrichmentData> fetchNutritionData(String code);
}

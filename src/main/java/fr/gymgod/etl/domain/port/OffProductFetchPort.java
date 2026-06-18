package fr.gymgod.etl.domain.port;

import fr.gymgod.etl.domain.model.OffProductData;

import java.util.Optional;

/**
 * Port pour la récupération en direct d'un produit auprès d'OpenFoodFacts
 * quand son code-barres est absent de la base locale ({@code products}) —
 * cas typique d'un scan en magasin d'un produit jamais importé.
 */
public interface OffProductFetchPort {

    /**
     * @param code code-barres OFF scanné par l'utilisateur
     * @return les données produit si trouvées sur OFF,
     *         {@link Optional#empty()} si la requête a échoué ou si le
     *         produit est introuvable.
     */
    Optional<OffProductData> fetchProduct(String code);
}

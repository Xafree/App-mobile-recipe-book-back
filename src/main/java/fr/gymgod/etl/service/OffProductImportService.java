package fr.gymgod.etl.service;

import fr.gymgod.common.entities.nutrition.Nutriment;
import fr.gymgod.common.entities.nutrition.Product;
import fr.gymgod.etl.domain.model.OffProductData;
import fr.gymgod.etl.domain.port.OffProductFetchPort;
import fr.gymgod.etl.domain.port.ProductDataPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Import à la demande d'un produit depuis OpenFoodFacts quand son
 * code-barres est absent de la base locale — cas typique d'un scan en
 * magasin d'un produit jamais importé par l'ETL.
 *
 * <p>Le produit récupéré est enregistré dans {@code products} pour que les
 * scans suivants du même code-barres soient servis depuis la base locale.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OffProductImportService {

    private final OffProductFetchPort offProductFetchPort;
    private final ProductDataPort productDataPort;

    /**
     * @param code code-barres scanné
     * @return le produit importé et enregistré, ou {@code null} si OFF ne
     *         connaît pas ce code-barres ou si la requête a échoué.
     */
    public Product importProduct(String code) {
        Optional<OffProductData> data = offProductFetchPort.fetchProduct(code);
        if (data.isEmpty()) {
            return null;
        }

        OffProductData d = data.get();
        Product product = new Product();
        product.setCode(code);
        product.setProductName(d.productName());
        product.setQuantity(d.quantity());
        product.setIngredientsText(d.ingredientsText());
        product.setNutriscore(d.nutriscore());
        product.setNutriscoreGrade(d.nutriscoreGrade());
        product.setNutriment(d.nutriment());
        product.setGlucide(d.glucide());
        product.setVitamin(d.vitamin());
        product.setNutritionDataIncomplete(!hasNutritionData(d.nutriment()));

        log.info("Produit {} importé à la demande depuis OpenFoodFacts (scan).", code);
        return productDataPort.save(product);
    }

    private boolean hasNutritionData(Nutriment n) {
        return n != null && (n.getEnergyKcal100g() > 0 || n.getProteins100g() > 0
                || n.getCarbohydrates100g() > 0 || n.getFat100g() > 0);
    }
}

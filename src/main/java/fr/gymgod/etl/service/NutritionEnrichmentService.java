package fr.gymgod.etl.service;

import fr.gymgod.common.entities.nutrition.Glucide;
import fr.gymgod.common.entities.nutrition.Nutriment;
import fr.gymgod.common.entities.nutrition.Product;
import fr.gymgod.common.entities.nutrition.Vitamin;
import fr.gymgod.etl.domain.model.NutritionEnrichmentData;
import fr.gymgod.etl.domain.port.NutritionEnrichmentPort;
import fr.gymgod.etl.domain.port.ProductDataPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Enrichissement à la demande des produits dont {@code nutritionDataIncomplete}
 * est à {@code true} (macros, minéraux ou vitamines absents de l'export CSV).
 *
 * <p>Appelé lors de la consultation d'un produit (cf. {@code OrchestratorNutrition.getProduct}) :
 * une seule requête OFF par produit suffit à compléter {@code Nutriment},
 * {@code Glucide} et {@code Vitamin} en une fois.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NutritionEnrichmentService {

    private final NutritionEnrichmentPort nutritionEnrichmentPort;
    private final ProductDataPort productDataPort;

    public Product enrichIfNeeded(Product product) {
        if (product == null || !product.isNutritionDataIncomplete()) {
            return product;
        }
        // Déjà tenté et échoué — on évite de re-déclencher un appel OFF à
        // chaque consultation (cf. pattern aiError sur l'enrichissement IA).
        if (product.getNutritionEnrichmentError() != null) {
            return product;
        }

        Optional<NutritionEnrichmentData> data = nutritionEnrichmentPort.fetchNutritionData(product.getCode());
        if (data.isEmpty()) {
            product.setNutritionEnrichmentError("OFF lookup failed or product not found");
            return productDataPort.save(product);
        }

        applyEnrichment(product, data.get());
        product.setNutritionDataIncomplete(false);

        log.info("Produit {} enrichi via l'API OpenFoodFacts.", product.getCode());
        return productDataPort.save(product);
    }

    /**
     * Copie les valeurs récupérées sur les sous-entités existantes (plutôt que
     * de remplacer la référence) pour préserver leur {@code id} et éviter une
     * ligne orpheline côté {@code OneToOne(cascade = ALL)}.
     */
    private void applyEnrichment(Product product, NutritionEnrichmentData data) {
        Nutriment nutriment = product.getNutriment() != null ? product.getNutriment() : new Nutriment();
        Nutriment fetched = data.nutriment();
        nutriment.setEnergyKcal100g(fetched.getEnergyKcal100g());
        nutriment.setProteins100g(fetched.getProteins100g());
        nutriment.setCarbohydrates100g(fetched.getCarbohydrates100g());
        nutriment.setFat100g(fetched.getFat100g());
        nutriment.setFiber100g(fetched.getFiber100g());
        nutriment.setSugars100g(fetched.getSugars100g());
        nutriment.setSaturatedFat100g(fetched.getSaturatedFat100g());
        product.setNutriment(nutriment);

        Glucide glucide = product.getGlucide() != null ? product.getGlucide() : new Glucide();
        Glucide fetchedGlucide = data.glucide();
        glucide.setSodium100g(fetchedGlucide.getSodium100g());
        glucide.setSalt100g(fetchedGlucide.getSalt100g());
        glucide.setPotassium100g(fetchedGlucide.getPotassium100g());
        glucide.setMagnesium100g(fetchedGlucide.getMagnesium100g());
        glucide.setCalcium100g(fetchedGlucide.getCalcium100g());
        product.setGlucide(glucide);

        Vitamin vitamin = product.getVitamin() != null ? product.getVitamin() : new Vitamin();
        Vitamin fetchedVitamin = data.vitamin();
        vitamin.setA100g(fetchedVitamin.getA100g());
        vitamin.setD100g(fetchedVitamin.getD100g());
        vitamin.setE100g(fetchedVitamin.getE100g());
        vitamin.setK100g(fetchedVitamin.getK100g());
        vitamin.setC100g(fetchedVitamin.getC100g());
        vitamin.setB1100g(fetchedVitamin.getB1100g());
        vitamin.setB2100g(fetchedVitamin.getB2100g());
        vitamin.setPp100g(fetchedVitamin.getPp100g());
        vitamin.setB6100g(fetchedVitamin.getB6100g());
        vitamin.setB9100g(fetchedVitamin.getB9100g());
        vitamin.setB12100g(fetchedVitamin.getB12100g());
        product.setVitamin(vitamin);
    }
}

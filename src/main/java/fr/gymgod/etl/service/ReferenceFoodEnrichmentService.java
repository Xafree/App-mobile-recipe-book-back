package fr.gymgod.etl.service;

import fr.gymgod.common.domain.nutrition.ReferenceFoodRepository;
import fr.gymgod.common.entities.nutrition.Country;
import fr.gymgod.common.entities.nutrition.Glucide;
import fr.gymgod.common.entities.nutrition.Nutriment;
import fr.gymgod.common.entities.nutrition.Product;
import fr.gymgod.common.entities.nutrition.ReferenceFood;
import fr.gymgod.common.entities.nutrition.ReferenceFoodSource;
import fr.gymgod.etl.domain.port.ProductDataPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.function.DoubleConsumer;

/**
 * Job batch hebdomadaire (cf. {@code ReferenceFoodEnrichmentJobAdapter}) : pour
 * les produits OFF bruts/génériques (sans marque) dont {@code Nutriment}/{@code
 * Glucide} sont manquants, recherche une correspondance par similarité de nom
 * dans {@code reference_foods} (CIQUAL/USDA) et complète les champs encore à
 * {@code 0.0} — sans jamais écraser une valeur OFF existante.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReferenceFoodEnrichmentService {

    private final ProductDataPort productDataPort;
    private final ReferenceFoodRepository referenceFoodRepository;

    private static final int BATCH_SIZE = 100;
    // Garde-fou : 200 lots de 100 = 20 000 produits par exécution hebdomadaire.
    private static final int MAX_BATCHES = 200;
    private static final double MG_TO_G = 1000.0;

    /** Noms de pays (France + voisins UE) pour lesquels CIQUAL est privilégié sur USDA. */
    private static final Set<String> CIQUAL_PRIORITY_COUNTRIES = Set.of(
            "france", "belgique", "belgium", "suisse", "switzerland", "allemagne", "germany",
            "espagne", "spain", "italie", "italy", "luxembourg", "pays-bas", "netherlands",
            "portugal", "autriche", "austria", "union européenne", "european union");

    public void processPendingBatch() {
        int totalProcessed = 0;
        int totalMatched = 0;

        for (int batch = 0; batch < MAX_BATCHES; batch++) {
            List<Product> pending = productDataPort.getPendingReferenceEnrichment(BATCH_SIZE);
            if (pending.isEmpty()) {
                break;
            }

            for (Product product : pending) {
                if (enrichProduct(product)) {
                    totalMatched++;
                }
                totalProcessed++;
            }
        }

        log.info("Reference food enrichment: {} produits traités, {} correspondances CIQUAL/USDA trouvées.",
                totalProcessed, totalMatched);
    }

    private boolean enrichProduct(Product product) {
        ReferenceFood match = findMatch(product);

        if (match != null) {
            applyMatch(product, match);
            product.setReferenceFoodMatch(match.getSource() + ":" + match.getSourceCode());
            product.setNutritionDataIncomplete(false);
        }

        product.setReferenceEnrichmentAttempted(true);
        productDataPort.save(product);
        return match != null;
    }

    private ReferenceFood findMatch(Product product) {
        String name = product.getProductName();
        if (name == null || name.isBlank()) {
            return null;
        }

        for (ReferenceFoodSource source : sourceOrder(product)) {
            ReferenceFood match = referenceFoodRepository.findBestMatch(name, source.name()).orElse(null);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    /** France/UE → CIQUAL d'abord, sinon USDA d'abord — l'autre source en repli. */
    private List<ReferenceFoodSource> sourceOrder(Product product) {
        List<Country> countries = product.getCountry();
        boolean ciqualFirst = countries == null || countries.isEmpty() || countries.stream()
                .map(Country::getName)
                .filter(java.util.Objects::nonNull)
                .map(String::toLowerCase)
                .anyMatch(CIQUAL_PRIORITY_COUNTRIES::contains);

        return ciqualFirst
                ? List.of(ReferenceFoodSource.CIQUAL, ReferenceFoodSource.USDA)
                : List.of(ReferenceFoodSource.USDA, ReferenceFoodSource.CIQUAL);
    }

    /**
     * Copie les valeurs per-100g de {@code food} vers {@code Nutriment}/{@code
     * Glucide}, uniquement sur les champs actuellement à {@code 0.0}.
     */
    private void applyMatch(Product product, ReferenceFood food) {
        Nutriment nutriment = product.getNutriment() != null ? product.getNutriment() : new Nutriment();
        fillIfZero(nutriment.getEnergyKcal100g(), food.getCaloriesPer100g(), nutriment::setEnergyKcal100g);
        fillIfZero(nutriment.getProteins100g(), food.getProteinPer100g(), nutriment::setProteins100g);
        fillIfZero(nutriment.getCarbohydrates100g(), food.getCarbsPer100g(), nutriment::setCarbohydrates100g);
        fillIfZero(nutriment.getFat100g(), food.getFatPer100g(), nutriment::setFat100g);
        fillIfZero(nutriment.getFiber100g(), food.getFiberPer100g(), nutriment::setFiber100g);
        fillIfZero(nutriment.getSugars100g(), food.getSugarPer100g(), nutriment::setSugars100g);
        fillIfZero(nutriment.getSaturatedFat100g(), food.getSaturatedFatPer100g(), nutriment::setSaturatedFat100g);
        product.setNutriment(nutriment);

        Glucide glucide = product.getGlucide() != null ? product.getGlucide() : new Glucide();
        fillIfZero(glucide.getSodium100g(), mgToG(food.getSodiumMgPer100g()), glucide::setSodium100g);
        fillIfZero(glucide.getPotassium100g(), mgToG(food.getPotassiumMgPer100g()), glucide::setPotassium100g);
        fillIfZero(glucide.getCalcium100g(), mgToG(food.getCalciumMgPer100g()), glucide::setCalcium100g);
        product.setGlucide(glucide);
    }

    private void fillIfZero(double currentValue, BigDecimal referenceValue, DoubleConsumer setter) {
        if (currentValue != 0.0 || referenceValue == null) {
            return;
        }
        setter.accept(referenceValue.doubleValue());
    }

    private BigDecimal mgToG(BigDecimal valueMg) {
        return valueMg == null ? null : valueMg.divide(BigDecimal.valueOf(MG_TO_G));
    }
}

package fr.gymgod.etl.service;

import fr.gymgod.common.domain.nutrition.ReferenceFoodRepository;
import fr.gymgod.common.entities.nutrition.Country;
import fr.gymgod.common.entities.nutrition.Glucide;
import fr.gymgod.common.entities.nutrition.Nutriment;
import fr.gymgod.common.entities.nutrition.Product;
import fr.gymgod.common.entities.nutrition.ReferenceFood;
import fr.gymgod.common.entities.nutrition.ReferenceFoodSource;
import fr.gymgod.etl.domain.port.ProductDataPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceFoodEnrichmentServiceTest {

    @Mock
    private ProductDataPort productDataPort;

    @Mock
    private ReferenceFoodRepository referenceFoodRepository;

    private ReferenceFoodEnrichmentService service;

    @BeforeEach
    void setUp() {
        service = new ReferenceFoodEnrichmentService(productDataPort, referenceFoodRepository);
    }

    private ReferenceFood ciqualBanane() {
        ReferenceFood food = new ReferenceFood();
        food.setSource(ReferenceFoodSource.CIQUAL);
        food.setSourceCode("CIQUAL_007");
        food.setName("Banane");
        food.setCaloriesPer100g(BigDecimal.valueOf(89));
        food.setProteinPer100g(BigDecimal.valueOf(1.1));
        food.setCarbsPer100g(BigDecimal.valueOf(20));
        food.setFatPer100g(BigDecimal.valueOf(0.3));
        food.setFiberPer100g(BigDecimal.valueOf(2.6));
        food.setSugarPer100g(BigDecimal.valueOf(12));
        food.setSaturatedFatPer100g(BigDecimal.valueOf(0.1));
        food.setSodiumMgPer100g(BigDecimal.valueOf(1));
        food.setPotassiumMgPer100g(BigDecimal.valueOf(360));
        food.setCalciumMgPer100g(BigDecimal.valueOf(5));
        food.setIronMgPer100g(BigDecimal.valueOf(0.3));
        return food;
    }

    @Test
    void produitSansMarqueAvecNomProcheDUneEntreeCiqualEstComplete() {
        Product product = new Product();
        product.setCode("123");
        product.setProductName("Banane bio");
        product.setNutritionDataIncomplete(true);

        when(productDataPort.getPendingReferenceEnrichment(100))
                .thenReturn(List.of(product))
                .thenReturn(List.of());
        when(referenceFoodRepository.findBestMatch("Banane bio", ReferenceFoodSource.CIQUAL.name()))
                .thenReturn(Optional.of(ciqualBanane()));

        service.processPendingBatch();

        Nutriment nutriment = product.getNutriment();
        assertThat(nutriment).isNotNull();
        assertThat(nutriment.getEnergyKcal100g()).isEqualTo(89.0);
        assertThat(nutriment.getProteins100g()).isEqualTo(1.1);
        assertThat(nutriment.getCarbohydrates100g()).isEqualTo(20.0);
        assertThat(nutriment.getFat100g()).isEqualTo(0.3);

        Glucide glucide = product.getGlucide();
        assertThat(glucide).isNotNull();
        // 360 mg/100g -> 0.36 g/100g
        assertThat(glucide.getPotassium100g()).isEqualTo(0.36);

        assertThat(product.isNutritionDataIncomplete()).isFalse();
        assertThat(product.isReferenceEnrichmentAttempted()).isTrue();
        assertThat(product.getReferenceFoodMatch()).isEqualTo("CIQUAL:CIQUAL_007");

        verify(productDataPort).save(product);
    }

    @Test
    void produitAvecValeurOffExistanteNEstPasEcrasee() {
        Product product = new Product();
        product.setCode("456");
        product.setProductName("Banane bio");
        product.setNutritionDataIncomplete(true);

        Nutriment existing = new Nutriment();
        existing.setEnergyKcal100g(95.0); // valeur OFF déjà présente
        product.setNutriment(existing);

        when(productDataPort.getPendingReferenceEnrichment(100))
                .thenReturn(List.of(product))
                .thenReturn(List.of());
        when(referenceFoodRepository.findBestMatch("Banane bio", ReferenceFoodSource.CIQUAL.name()))
                .thenReturn(Optional.of(ciqualBanane()));

        service.processPendingBatch();

        // L'énergie OFF existante n'est pas écrasée par la valeur CIQUAL.
        assertThat(product.getNutriment().getEnergyKcal100g()).isEqualTo(95.0);
        // Les autres champs, à 0.0, sont complétés.
        assertThat(product.getNutriment().getProteins100g()).isEqualTo(1.1);
    }

    @Test
    void aucuneCorrespondanceMarqueAttemptedSansModifierLesMacros() {
        Product product = new Product();
        product.setCode("789");
        product.setProductName("Produit inconnu xyz");
        product.setNutritionDataIncomplete(true);

        when(productDataPort.getPendingReferenceEnrichment(100))
                .thenReturn(List.of(product))
                .thenReturn(List.of());
        when(referenceFoodRepository.findBestMatch(eq("Produit inconnu xyz"), anyString()))
                .thenReturn(Optional.empty());

        service.processPendingBatch();

        assertThat(product.isReferenceEnrichmentAttempted()).isTrue();
        assertThat(product.isNutritionDataIncomplete()).isTrue();
        assertThat(product.getReferenceFoodMatch()).isNull();
        assertThat(product.getNutriment()).isNull();

        verify(productDataPort).save(product);
    }

    @Test
    void ordreDesSourcesDependDuPays() {
        Product product = new Product();
        product.setCode("999");
        product.setProductName("Chicken breast");
        product.setNutritionDataIncomplete(true);
        Country usa = new Country();
        usa.setName("united states");
        product.setCountry(List.of(usa));

        when(productDataPort.getPendingReferenceEnrichment(100))
                .thenReturn(List.of(product))
                .thenReturn(List.of());
        when(referenceFoodRepository.findBestMatch(anyString(), eq(ReferenceFoodSource.USDA.name())))
                .thenReturn(Optional.empty());
        when(referenceFoodRepository.findBestMatch(anyString(), eq(ReferenceFoodSource.CIQUAL.name())))
                .thenReturn(Optional.empty());

        service.processPendingBatch();

        // USDA doit être tenté en premier pour un produit hors France/UE.
        verify(referenceFoodRepository).findBestMatch("Chicken breast", ReferenceFoodSource.USDA.name());
    }
}

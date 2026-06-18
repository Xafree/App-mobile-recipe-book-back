package fr.gymgod.etl.service;

import fr.gymgod.common.domain.nutrition.ReferenceFoodRepository;
import fr.gymgod.common.entities.nutrition.ReferenceFood;
import fr.gymgod.common.entities.nutrition.ReferenceFoodSource;
import fr.gymgod.etl.service.referencefood.CiqualImportService;
import fr.gymgod.etl.service.referencefood.UsdaImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceFoodImportServiceTest {

    @Mock
    private ReferenceFoodFileService referenceFoodFileService;

    @Mock
    private CiqualImportService ciqualImportService;

    @Mock
    private UsdaImportService usdaImportService;

    @Mock
    private ReferenceFoodRepository referenceFoodRepository;

    private ReferenceFoodImportService service;

    @BeforeEach
    void setUp() {
        service = new ReferenceFoodImportService(referenceFoodFileService, ciqualImportService, usdaImportService,
                referenceFoodRepository);
        ReflectionTestUtils.setField(service, "ciqualUrl", "http://ciqual");
        ReflectionTestUtils.setField(service, "ciqualFile", "data/ciqual.xlsx");
        ReflectionTestUtils.setField(service, "usdaUrl", "http://usda");
        ReflectionTestUtils.setField(service, "usdaFile", "data/usda.zip");
    }

    private ReferenceFood banane() {
        ReferenceFood food = new ReferenceFood();
        food.setSource(ReferenceFoodSource.CIQUAL);
        food.setSourceCode("13001");
        food.setName("Banane, pulpe, crue");
        food.setCategory("fruits");
        food.setCaloriesPer100g(BigDecimal.valueOf(89));
        return food;
    }

    @Test
    void importAllUpsertParSourceEtSourceCodeSansDoublon() throws Exception {
        when(referenceFoodFileService.ensureFileAvailable("http://ciqual", "data/ciqual.xlsx"))
                .thenReturn("data/ciqual.xlsx");
        when(referenceFoodFileService.ensureFileAvailable("http://usda", "data/usda.zip"))
                .thenReturn("data/usda.zip");
        when(ciqualImportService.parse("data/ciqual.xlsx")).thenReturn(List.of(banane()));
        when(usdaImportService.parse("data/usda.zip")).thenReturn(List.of());

        // Premier import : aucune entrée existante.
        when(referenceFoodRepository.findBySourceAndSourceCode(ReferenceFoodSource.CIQUAL, "13001"))
                .thenReturn(Optional.empty());

        service.importAll();

        verify(referenceFoodRepository, times(1)).save(any(ReferenceFood.class));
    }

    @Test
    void importAllMetAJourLEntreeExistanteSansEnCreerUneNouvelle() throws Exception {
        when(referenceFoodFileService.ensureFileAvailable("http://ciqual", "data/ciqual.xlsx"))
                .thenReturn("data/ciqual.xlsx");
        when(referenceFoodFileService.ensureFileAvailable("http://usda", "data/usda.zip"))
                .thenReturn("data/usda.zip");
        when(ciqualImportService.parse("data/ciqual.xlsx")).thenReturn(List.of(banane()));
        when(usdaImportService.parse("data/usda.zip")).thenReturn(List.of());

        ReferenceFood existing = new ReferenceFood();
        existing.setSource(ReferenceFoodSource.CIQUAL);
        existing.setSourceCode("13001");
        existing.setName("Ancien nom");
        when(referenceFoodRepository.findBySourceAndSourceCode(ReferenceFoodSource.CIQUAL, "13001"))
                .thenReturn(Optional.of(existing));

        service.importAll();

        assertThat(existing.getName()).isEqualTo("Banane, pulpe, crue");
        assertThat(existing.getCaloriesPer100g()).isEqualTo(BigDecimal.valueOf(89));
        verify(referenceFoodRepository, times(1)).save(existing);
    }

    @Test
    void siLeFichierEstIndisponibleAucunUpsertNEstFait() throws Exception {
        when(referenceFoodFileService.ensureFileAvailable("http://ciqual", "data/ciqual.xlsx"))
                .thenReturn(null);
        when(referenceFoodFileService.ensureFileAvailable("http://usda", "data/usda.zip"))
                .thenReturn("data/usda.zip");
        when(usdaImportService.parse("data/usda.zip")).thenReturn(List.of());

        service.importAll();

        verify(referenceFoodRepository, never()).save(any(ReferenceFood.class));
        verify(ciqualImportService, never()).parse(any());
    }
}

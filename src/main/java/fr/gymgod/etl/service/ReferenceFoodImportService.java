package fr.gymgod.etl.service;

import fr.gymgod.common.domain.nutrition.ReferenceFoodRepository;
import fr.gymgod.common.entities.nutrition.ReferenceFood;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

import fr.gymgod.etl.service.referencefood.CiqualImportService;
import fr.gymgod.etl.service.referencefood.UsdaImportService;

/**
 * Orchestre le (re)chargement complet du référentiel CIQUAL + USDA Foundation
 * Foods dans {@code reference_foods} : téléchargement (si nécessaire) puis
 * parsing puis upsert idempotent par {@code (source, sourceCode)}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReferenceFoodImportService {

    private final ReferenceFoodFileService referenceFoodFileService;
    private final CiqualImportService ciqualImportService;
    private final UsdaImportService usdaImportService;
    private final ReferenceFoodRepository referenceFoodRepository;

    @Value("${path.ciqual.url}")
    private String ciqualUrl;

    @Value("${path.ciqual.file}")
    private String ciqualFile;

    @Value("${path.usda.url}")
    private String usdaUrl;

    @Value("${path.usda.file}")
    private String usdaFile;

    public void importAll() {
        importSource("CIQUAL", ciqualUrl, ciqualFile, this::parseCiqual);
        importSource("USDA", usdaUrl, usdaFile, this::parseUsda);
    }

    private List<ReferenceFood> parseCiqual(String path) throws Exception {
        return ciqualImportService.parse(path);
    }

    private List<ReferenceFood> parseUsda(String path) throws Exception {
        return usdaImportService.parse(path);
    }

    private void importSource(String label, String url, String localFile, Parser parser) {
        try {
            String path = referenceFoodFileService.ensureFileAvailable(url, localFile);
            if (path == null) {
                log.error("{} : fichier de référence indisponible, import ignoré.", label);
                return;
            }

            List<ReferenceFood> parsed = parser.parse(path);
            int upserted = upsert(parsed);
            log.info("{} : {} aliments importés dans reference_foods.", label, upserted);
        } catch (Exception e) {
            log.error("Échec de l'import {} : {}", label, e.getMessage(), e);
        }
    }

    private int upsert(List<ReferenceFood> parsed) {
        int count = 0;
        for (ReferenceFood incoming : parsed) {
            ReferenceFood food = referenceFoodRepository
                    .findBySourceAndSourceCode(incoming.getSource(), incoming.getSourceCode())
                    .orElseGet(ReferenceFood::new);

            food.setSource(incoming.getSource());
            food.setSourceCode(incoming.getSourceCode());
            food.setName(incoming.getName());
            food.setCategory(incoming.getCategory());
            food.setCaloriesPer100g(incoming.getCaloriesPer100g());
            food.setProteinPer100g(incoming.getProteinPer100g());
            food.setCarbsPer100g(incoming.getCarbsPer100g());
            food.setFatPer100g(incoming.getFatPer100g());
            food.setFiberPer100g(incoming.getFiberPer100g());
            food.setSugarPer100g(incoming.getSugarPer100g());
            food.setSaturatedFatPer100g(incoming.getSaturatedFatPer100g());
            food.setSodiumMgPer100g(incoming.getSodiumMgPer100g());
            food.setPotassiumMgPer100g(incoming.getPotassiumMgPer100g());
            food.setCalciumMgPer100g(incoming.getCalciumMgPer100g());
            food.setIronMgPer100g(incoming.getIronMgPer100g());

            referenceFoodRepository.save(food);
            count++;
        }
        return count;
    }

    @FunctionalInterface
    private interface Parser {
        List<ReferenceFood> parse(String path) throws Exception;
    }
}

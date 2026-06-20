package fr.gymgod.etl.service;

import fr.gymgod.common.entities.nutrition.Country;
import fr.gymgod.common.entities.nutrition.*;
import fr.gymgod.etl.domain.service.EtlDataParser;
import fr.gymgod.etl.domain.port.ProductDataPort;
import fr.gymgod.etl.controller.event.ProductImageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EtlBatchProcessor {

    private final ProductDataPort productDataPort;
    private final EtlReferenceCacheService cacheService;
    private final ProductMapper productMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${ai.enabled}")
    private boolean aiEnabled;

    @Transactional
    public BatchResult processBatch(List<String> lines, String version) {
        List<String[]> batchColumns = new ArrayList<>(lines.size());
        List<String> codes = new ArrayList<>();

        for (String line : lines) {
            String[] columns = line.split("\t", -1);
            if (columns.length < 1) {
                continue;
            }

            String code = EtlDataParser.getValue(columns, 0);
            if (code == null || code.length() > 50) {
                continue;
            }

            batchColumns.add(columns);
            codes.add(code);
        }

        // 1. Chargement des produits existants
        Map<String, Product> existingMap = new HashMap<>();
        if (!codes.isEmpty()) {
            List<Product> existingProducts = productDataPort.getAllByCode(codes);
            for (Product p : existingProducts) {
                existingMap.put(p.getCode(), p);
            }
        }

        // 2. Résolution des références (cache global et canonique)
        Map<String, Brand> brandCache = cacheService.resolveBrands(batchColumns);
        Map<String, Categorie> categorieCache = cacheService.resolveCategories(batchColumns);
        Map<String, Country> countryCache = cacheService.resolveCountries(batchColumns);
        Map<String, Label> labelCache = cacheService.resolveLabels(batchColumns);
        Map<String, Ingredient> ingredientCache = cacheService.resolveIngredients(batchColumns);
        Map<String, Trace> traceCache = cacheService.resolveTraces(batchColumns);

        List<Product> productsToSave = new ArrayList<>();
        Product lastProduct = null;
        int created = 0, updated = 0, skipped = 0;

        for (String[] columns : batchColumns) {
            try {
                String code = EtlDataParser.getValue(columns, 0);
                Product existing = existingMap.get(code);

                String createdTimeInput = EtlDataParser.getValue(columns, 3);
                String lastModifiedTimeInput = EtlDataParser.getValue(columns, 5);

                // RESUME LOGIC : vérification timestamp + statut AI
                boolean isUpToDate = existing != null &&
                        isSame(existing.getCreatedTime(), createdTimeInput) &&
                        isSame(existing.getLastModifiedTime(), lastModifiedTimeInput);

                if (isUpToDate && aiEnabled && existing != null && !existing.isAiEnriched()) {
                    isUpToDate = false;
                }

                if (isUpToDate) {
                    skipped++;
                    continue;
                }

                if (existing == null) {
                    created++;
                } else {
                    updated++;
                }

                Product p = productMapper.mapToProduct(columns, existing, brandCache, categorieCache, countryCache,
                        labelCache, ingredientCache, traceCache, version);

                productsToSave.add(p);
                lastProduct = p;
            } catch (Exception e) {
                log.error("Erreur lors du traitement d'une ligne TSV", e);
            }
        }

        if (!productsToSave.isEmpty()) {
            log.info("[Batch] {} créés | {} mis à jour | {} ignorés (à jour) | {} lignes dans le batch",
                    created, updated, skipped, lines.size());
            List<Product> savedProducts = this.productDataPort.saveAll(productsToSave);
            log.info("[DB] {} produits sauvegardés en base", savedProducts.size());

            int imagesTriggered = 0;
            for (Product p : savedProducts) {
                if (p.getImage() != null) {
                    eventPublisher.publishEvent(new ProductImageEvent(this, p.getCode(), p.getImage()));
                    imagesTriggered++;
                }
            }
            if (imagesTriggered > 0) {
                log.info("[Images] {} événements de téléchargement d'images déclenchés", imagesTriggered);
            }
        } else {
            log.debug("[Batch] Aucun produit à créer/mettre à jour sur {} lignes (tous à jour)", lines.size());
        }

        return new BatchResult(created, updated, skipped, lastProduct);
    }

    private boolean isSame(String s1, String s2) {
        if (s1 == null) {
            return s2 == null;
        }
        return s1.equals(s2);
    }
}

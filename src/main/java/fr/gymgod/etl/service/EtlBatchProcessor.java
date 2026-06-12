package fr.gymgod.etl.service;

import fr.gymgod.common.entities.nutrition.Country;
import fr.gymgod.common.entities.nutrition.*;
import fr.gymgod.common.entities.user.UserAccount;
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
    public Product processBatch(List<String> lines, String version) {
        List<String[]> batchColumns = new ArrayList<>(lines.size());
        List<String> codes = new ArrayList<>();

        for (String line : lines) {
            String[] columns = line.split("\t", -1);
            if (columns.length < 1)
                continue;

            String code = EtlDataParser.getValue(columns, 0);
            if (code == null || code.length() > 50) {
                continue;
            }

            batchColumns.add(columns);
            codes.add(code);
        }

        // 1. Load existing products
        Map<String, Product> existingMap = new HashMap<>();
        if (!codes.isEmpty()) {
            List<Product> existingProducts = productDataPort.getAllByCode(codes);
            for (Product p : existingProducts) {
                existingMap.put(p.getCode(), p);
            }
        }

        // 2. Reference resolution (Global and Canonical Cache)
        Map<String, Brand> brandCache = cacheService.resolveBrands(batchColumns);
        Map<String, Categorie> categorieCache = cacheService.resolveCategories(batchColumns);
        Map<String, Country> countryCache = cacheService.resolveCountries(batchColumns);
        Map<String, Label> labelCache = cacheService.resolveLabels(batchColumns);
        Map<String, Ingredient> ingredientCache = cacheService.resolveIngredients(batchColumns);
        Map<String, Trace> traceCache = cacheService.resolveTraces(batchColumns);

        List<Product> productsToSave = new ArrayList<>();
        Product lastProduct = null;

        for (String[] columns : batchColumns) {
            try {
                String code = EtlDataParser.getValue(columns, 0);
                Product existing = existingMap.get(code);

                String createdTimeInput = EtlDataParser.getValue(columns, 3);
                String lastModifiedTimeInput = EtlDataParser.getValue(columns, 5);

                // RESUME LOGIC: Check timestamp AND AI status
                boolean isUpToDate = existing != null &&
                        isSame(existing.getCreatedTime(), createdTimeInput) &&
                        isSame(existing.getLastModifiedTime(), lastModifiedTimeInput);

                // If AI is enabled, we also require the product to be AI enriched
                if (isUpToDate && aiEnabled && existing != null && !existing.isAiEnriched()) {
                    isUpToDate = false; // Force update to run AI
                }

                if (isUpToDate) {
                    continue;
                }

                Product p = productMapper.mapToProduct(columns, existing, brandCache, categorieCache, countryCache,
                        labelCache, ingredientCache, traceCache, version);

                productsToSave.add(p);
                lastProduct = p;
            } catch (Exception e) {
                log.error("Error processing line for TSV code", e);
            }
        }

        if (!productsToSave.isEmpty()) {
            log.info("Batch summary: {} products processed to create/update out of {} total lines.",
                    productsToSave.size(), lines.size());
            List<Product> savedProducts = this.productDataPort.saveAll(productsToSave);

            // Trigger Async Image Download
            int imagesTriggered = 0;
            for (Product p : savedProducts) {
                if (p.getImage() != null) {
                    eventPublisher
                            .publishEvent(new ProductImageEvent(this, p.getCode(),
                                    p.getImage()));
                    imagesTriggered++;
                }
            }
            log.info("Batch image processing: {} image download events triggered.", imagesTriggered);
        } else {
            log.debug("Batch summary: 0 products to update/create out of {} total lines.", lines.size());
        }
        return lastProduct;
    }

    private boolean isSame(String s1, String s2) {
        if (s1 == null)
            return s2 == null;
        return s1.equals(s2);
    }
}

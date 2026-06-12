package fr.gymgod.etl.service;

import fr.gymgod.etl.domain.model.AdditiveData;
import fr.gymgod.etl.domain.port.AiEnrichmentPort;
import fr.gymgod.etl.domain.port.ProductDataPort;
import fr.gymgod.etl.domain.port.ReferenceDataPort;
import fr.gymgod.common.entities.nutrition.Additive;
import fr.gymgod.common.entities.nutrition.Ingredient;
import fr.gymgod.common.entities.nutrition.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestratorAiEnrichmentService {

    private final ProductDataPort productDataPort;
    private final AiEnrichmentPort aiEnrichmentPort;
    private final ReferenceDataPort referenceDataPort;
    private final PlatformTransactionManager transactionManager;

    private static final int BATCH_SIZE = 50;
    private static final int THREAD_POOL_SIZE = 3;

    private final ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    public void processPendingBatch() {
        List<Product> pendingProducts = productDataPort.getPendingAiEnrichment(BATCH_SIZE);

        if (pendingProducts.isEmpty()) {
            return;
        }

        log.info("AI Enrichment: Found {} products to enrich. Launching parallel processing ({} threads)...",
                pendingProducts.size(), THREAD_POOL_SIZE);

        List<CompletableFuture<Void>> futures = pendingProducts.stream()
                .map(p -> CompletableFuture.runAsync(() -> {
                    try {
                        TransactionTemplate template = new TransactionTemplate(transactionManager);
                        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

                        template.execute(status -> {
                            enrichProduct(p);
                            return null;
                        });
                    } catch (Exception e) {
                        log.error("Failed to enrich product {}", p.getCode(), e);
                    }
                }, executor))
                .toList();

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("AI Enrichment: Batch processing completed.");
    }

    private void enrichProduct(Product productDetached) {
        Product p = productDataPort.get(productDetached.getCode());
        if (p == null)
            return;

        String rawIngredients = p.getIngredientsText();
        if (rawIngredients == null || rawIngredients.isBlank()) {
            p.setAiEnriched(true);
            productDataPort.save(p);
            return;
        }

        log.info("Enriching product {} with AI...", p.getCode());

        try {
            // 1. Clean Ingredients
            List<String> cleanedNames = aiEnrichmentPort.cleanIngredients(rawIngredients).join();

            // Deduplicate names to avoid Multiple representations error
            List<String> uniqueNames = cleanedNames.stream().distinct().toList();

            // 2. Extract Additives
            List<AdditiveData> additiveContents = aiEnrichmentPort.extractAdditives(uniqueNames).join();

            // 3. Resolve Entities via ReferenceDataPort and Update Product
            if (!uniqueNames.isEmpty()) {
                List<Ingredient> ingredients = referenceDataPort.getOrCreateIngredients(uniqueNames);
                p.setIngredient(ingredients);
            }
            if (!additiveContents.isEmpty()) {
                List<Additive> additives = referenceDataPort.getOrCreateAdditives(additiveContents);
                p.setAdditives(additives);
            }

            p.setAiEnriched(true);
            productDataPort.save(p);
            log.info("Product {} enriched successfully.", p.getCode());

        } catch (Exception e) {
            Throwable cause = e;
            if (e instanceof java.util.concurrent.CompletionException && e.getCause() != null) {
                cause = e.getCause();
            }
            String errorMessage = cause.getMessage();

            log.warn("AI enrichment failed for product {}: {}", p.getCode(), errorMessage);

            try {
                p.setAiError(errorMessage);
                p.setAiEnriched(true); // Mark as processed to stop retry loop
                productDataPort.save(p);
            } catch (Exception saveEx) {
                log.error("Failed to save error status for product {}", p.getCode(), saveEx);
            }
        }
    }
}

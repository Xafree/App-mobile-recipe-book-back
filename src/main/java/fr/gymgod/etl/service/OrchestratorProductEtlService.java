package fr.gymgod.etl.service;

import fr.gymgod.common.entities.nutrition.Product;
import fr.gymgod.etl.domain.port.ProductDataPort;
import fr.gymgod.etl.domain.port.ReferenceDataPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestratorProductEtlService {

    private final EtlFileService etlFileService;
    private final EtlBatchProcessor etlBatchProcessor;
    private final EtlSchemaValidator schemaValidator;
    private final EtlStatusHolder statusHolder;
    private final ReferenceDataPort referenceDataPort;
    private final ProductDataPort productDataPort;

    private static final int BATCH_SIZE = 1000;
    private static final int THREAD_POOL_SIZE = 4;

    public void loadData(String filePath) {
        // ── Étape 1 : vérification / téléchargement du fichier ──────────────────
        String version = etlFileService.ensureFileAvailableAndGetVersion(filePath);

        String finalFilePath = (filePath == null || filePath.isEmpty() || filePath.equals("auto"))
                ? etlFileService.getEtlFilePath()
                : filePath;

        logFileInfo(finalFilePath);

        // ── Étape 2 : initialisation ─────────────────────────────────────────────
        final String importVersion = version;
        long startTime = System.currentTimeMillis();

        AtomicInteger totalLines   = new AtomicInteger(0);
        AtomicInteger totalCreated = new AtomicInteger(0);
        AtomicInteger totalUpdated = new AtomicInteger(0);
        AtomicInteger totalSkipped = new AtomicInteger(0);
        AtomicReference<Product> lastProductRef = new AtomicReference<>();

        List<String> batchLines = new ArrayList<>(BATCH_SIZE);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        Semaphore semaphore = new Semaphore(THREAD_POOL_SIZE * 2);

        log.info("═══ ETL démarré — fichier : {} | version : {} ═══", finalFilePath, importVersion);

        // ── Étape 3 : lecture ligne par ligne ────────────────────────────────────
        try (BufferedReader br = new BufferedReader(new FileReader(finalFilePath))) {
            String header = br.readLine();
            if (header == null || !schemaValidator.validate(header)) {
                log.error("═══ ETL annulé : le format du fichier ne correspond pas à celui attendu par ProductMapper ═══");
                statusHolder.record(EtlRunStats.failure(finalFilePath, "Format du fichier invalide ou non conforme au schéma attendu"));
                return;
            }

            String line;
            while ((line = br.readLine()) != null) {
                batchLines.add(line);

                if (batchLines.size() >= BATCH_SIZE) {
                    submitBatch(batchLines, importVersion, executor, semaphore,
                            totalLines, totalCreated, totalUpdated, totalSkipped, lastProductRef);
                    batchLines = new ArrayList<>(BATCH_SIZE);
                }
            }

            // Dernier batch partiel
            if (!batchLines.isEmpty()) {
                submitBatch(batchLines, importVersion, executor, semaphore,
                        totalLines, totalCreated, totalUpdated, totalSkipped, lastProductRef);
            }

            // Attente de fin de tous les batches
            semaphore.acquire(THREAD_POOL_SIZE * 2);

        } catch (Exception e) {
            log.error("Erreur critique lors de la lecture du fichier ETL", e);
            statusHolder.record(EtlRunStats.failure(finalFilePath, e.getMessage()));
            return;
        } finally {
            executor.shutdown();
        }

        // ── Étape 4 : récapitulatif final ────────────────────────────────────────
        long elapsedS = (System.currentTimeMillis() - startTime) / 1000;
        log.info("═══ ETL terminé en {} s ═══", elapsedS);
        log.info("  Lignes lues     : {}", totalLines.get());
        log.info("  Créés           : {}", totalCreated.get());
        log.info("  Mis à jour      : {}", totalUpdated.get());
        log.info("  Ignorés (à jour): {}", totalSkipped.get());
        log.info("  Pays manquants  : {}", referenceDataPort.getCountryMissing());

        statusHolder.record(EtlRunStats.success(finalFilePath, importVersion, elapsedS,
                totalLines.get(), totalCreated.get(), totalUpdated.get(), totalSkipped.get()));
    }

    private void submitBatch(List<String> batch,
                             String version,
                             ExecutorService executor,
                             Semaphore semaphore,
                             AtomicInteger totalLines,
                             AtomicInteger totalCreated,
                             AtomicInteger totalUpdated,
                             AtomicInteger totalSkipped,
                             AtomicReference<Product> lastProductRef) throws InterruptedException {

        semaphore.acquire();
        final List<String> currentBatch = batch;

        CompletableFuture
                .supplyAsync(() -> etlBatchProcessor.processBatch(currentBatch, version), executor)
                .thenAccept(result -> {
                    if (result.lastProduct() != null) {
                        lastProductRef.set(result.lastProduct());
                    }

                    totalCreated.addAndGet(result.created());
                    totalUpdated.addAndGet(result.updated());
                    totalSkipped.addAndGet(result.skipped());

                    int processed = totalLines.addAndGet(currentBatch.size());
                    if (processed % 10_000 == 0) {
                        log.info("[Progression] {} lignes traitées — {} créés | {} mis à jour | {} ignorés",
                                processed, totalCreated.get(), totalUpdated.get(), totalSkipped.get());
                    }

                    semaphore.release();
                })
                .exceptionally(e -> {
                    log.error("Erreur lors du traitement d'un batch", e);
                    semaphore.release();
                    return null;
                });
    }

    private void logFileInfo(String filePath) {
        try {
            long sizeBytes = Files.size(Paths.get(filePath));
            double sizeMb = sizeBytes / (1024.0 * 1024.0);
            log.info("[Fichier] {} — taille : {}", filePath,
                    sizeMb >= 1024 ? String.format("%.1f Go", sizeMb / 1024) : String.format("%.1f Mo", sizeMb));
        } catch (Exception e) {
            log.warn("[Fichier] Impossible de lire la taille de {}", filePath);
        }
    }
}

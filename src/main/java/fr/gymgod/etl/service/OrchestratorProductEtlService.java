package fr.gymgod.etl.service;

import fr.gymgod.common.entities.nutrition.Product;
import fr.gymgod.etl.domain.port.ProductDataPort;
import fr.gymgod.etl.domain.port.ReferenceDataPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
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
    private final ReferenceDataPort referenceDataPort;
    private final ProductDataPort productDataPort;

    private static final int BATCH_SIZE = 1000;
    private static final int THREAD_POOL_SIZE = 4;

    public void loadData(String filePath) {
        String version = etlFileService.ensureFileAvailableAndGetVersion(filePath);

        String finalFilePath = (filePath == null || filePath.isEmpty() || filePath.equals("auto"))
                ? etlFileService.getEtlFilePath()
                : filePath;

        final String importVersion = version;

        long startTime = System.currentTimeMillis();
        AtomicInteger count = new AtomicInteger(0);
        List<String> batchLines = new ArrayList<>(BATCH_SIZE);
        AtomicReference<Product> lastProductRef = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        // Semaphore to limit the number of pending tasks and avoid OOM
        Semaphore semaphore = new Semaphore(THREAD_POOL_SIZE * 2);

        log.info("Starting ETL for file: {}", finalFilePath);

        try (BufferedReader br = new BufferedReader(new FileReader(finalFilePath))) {
            String line;
            br.readLine(); // Skip header

            while ((line = br.readLine()) != null) {
                batchLines.add(line);

                if (batchLines.size() >= BATCH_SIZE) {
                    List<String> currentBatch = new ArrayList<>(batchLines);
                    batchLines.clear();

                    semaphore.acquire(); // Blocks if too many tasks are in progress

                    CompletableFuture
                            .supplyAsync(() -> etlBatchProcessor.processBatch(currentBatch, importVersion), executor)
                            .thenAccept(p -> {
                                if (p != null)
                                    lastProductRef.set(p);
                                int currentCount = count.addAndGet(currentBatch.size());
                                if (currentCount % 10000 == 0) {
                                    log.info("Processed products: {}", currentCount);
                                }
                                semaphore.release();
                            }).exceptionally(e -> {
                                log.error("Batch processing error", e);
                                semaphore.release();
                                return null;
                            });
                }
            }

            if (!batchLines.isEmpty()) {
                List<String> currentBatch = new ArrayList<>(batchLines);
                semaphore.acquire();
                CompletableFuture
                        .supplyAsync(() -> etlBatchProcessor.processBatch(currentBatch, importVersion), executor)
                        .thenAccept(p -> {
                            if (p != null)
                                lastProductRef.set(p);
                            count.addAndGet(currentBatch.size());
                            semaphore.release();
                        }).exceptionally(e -> {
                            log.error("Final batch processing error", e);
                            semaphore.release();
                            return null;
                        });
            }

            // Wait for all processing to finish by acquiring all permits
            semaphore.acquire(THREAD_POOL_SIZE * 2);

        } catch (Exception e) {
            log.error("Critical read error", e);
            return;
        } finally {
            executor.shutdown();
        }

        long endTime = System.currentTimeMillis();

        log.info("Execution time: {} ms", (endTime - startTime));
        log.info("Missing countries: {}", this.referenceDataPort.getCountryMissing());
    }
}

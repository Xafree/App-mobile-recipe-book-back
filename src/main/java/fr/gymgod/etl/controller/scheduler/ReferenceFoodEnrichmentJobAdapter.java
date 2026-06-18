package fr.gymgod.etl.controller.scheduler;

import fr.gymgod.etl.service.ReferenceFoodEnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job hebdomadaire : complète les {@code Nutriment}/{@code Glucide} des
 * produits OFF bruts/génériques dont les macros sont manquantes, par
 * correspondance avec les tables de référence CIQUAL/USDA
 * ({@code reference_foods}). Cf. {@link ReferenceFoodEnrichmentService}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReferenceFoodEnrichmentJobAdapter {

    private final ReferenceFoodEnrichmentService referenceFoodEnrichmentService;

    @Scheduled(cron = "0 0 3 * * SUN") // Tous les dimanches à 3h
    public void processWeeklyReferenceEnrichment() {
        try {
            referenceFoodEnrichmentService.processPendingBatch();
        } catch (Exception e) {
            log.error("Reference food enrichment batch failed at the scheduler level.", e);
        }
    }
}

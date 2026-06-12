package fr.gymgod.etl.controller.scheduler;

import fr.gymgod.etl.service.OrchestratorAiEnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiEnrichmentJobAdapter {

    private final OrchestratorAiEnrichmentService orchestratorAiEnrichmentService;

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Scheduled(initialDelay = 60000, fixedDelay = 60000) // Wait 1 min after startup, then every minute
    public void processPendingAiEnrichment() {
        if (!aiEnabled) {
            log.trace("AI Enrichment is disabled by configuration (ai.enabled). Skipping execution.");
            return;
        }

        try {
            orchestratorAiEnrichmentService.processPendingBatch();
        } catch (Exception e) {
            log.error("AI Enrichment processing batch failed at the scheduler level.", e);
        }
    }
}

package fr.gymgod.etl.controller.startup;

import fr.gymgod.etl.service.OrchestratorAdditiveLoaderService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdditiveLoaderAdapter {

    private final OrchestratorAdditiveLoaderService orchestratorAdditiveLoaderService;

    @PostConstruct
    public void init() {
        log.info("Startup phase: trigger additive loading");
        try {
            orchestratorAdditiveLoaderService.loadAdditives();
        } catch (Exception e) {
            log.error("Failed to load additives during startup", e);
        }
    }
}

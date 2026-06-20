package fr.gymgod.etl.controller.scheduler;

import fr.gymgod.etl.service.OrchestratorProductEtlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Désactivé par défaut ({@code etl.products.startup.enabled=false}) — l'import
 * complet du dump OpenFoodFacts (plusieurs millions de lignes) n'a pas
 * vocation à tourner à chaque démarrage local ; l'activer explicitement quand
 * un (re)import est réellement souhaité.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test") // Do not run during tests
@ConditionalOnProperty(name = "etl.products.startup.enabled", havingValue = "true", matchIfMissing = false)
public class EtlStartupRunner implements CommandLineRunner {

    private final OrchestratorProductEtlService etlService;

    @Override
    public void run(String... args) {
        log.info("Automatic startup of the product ETL...");
        try {
            // "auto" triggers download if needed, or uses local file
            etlService.loadData("auto");
        } catch (Exception e) {
            log.error("Error launching ETL at startup", e);
        }
    }
}

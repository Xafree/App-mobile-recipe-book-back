package fr.gymgod.etl.controller.startup;

import fr.gymgod.etl.service.ReferenceFoodImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Au démarrage : télécharge (si absent ou de plus de 7 jours) et importe le
 * référentiel complet CIQUAL + USDA Foundation Foods dans
 * {@code reference_foods}, consommé par {@code ReferenceFoodEnrichmentService}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class ReferenceFoodImportStartupRunner implements CommandLineRunner {

    private final ReferenceFoodImportService referenceFoodImportService;

    @Override
    public void run(String... args) {
        log.info("Startup phase: importing reference food data (CIQUAL/USDA Foundation Foods)...");
        try {
            referenceFoodImportService.importAll();
        } catch (Exception e) {
            log.error("Error importing reference food data at startup", e);
        }
    }
}

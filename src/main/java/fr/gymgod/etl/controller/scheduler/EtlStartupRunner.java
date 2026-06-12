package fr.gymgod.etl.controller.scheduler;

import fr.gymgod.etl.service.OrchestratorProductEtlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test") // Do not run during tests
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

package fr.gymgod.etl.service;

import fr.gymgod.etl.domain.model.AdditiveData;
import fr.gymgod.etl.domain.port.AdditiveSourcePort;
import fr.gymgod.etl.domain.port.ReferenceDataPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestratorAdditiveLoaderService {

    private final AdditiveSourcePort additiveSourcePort;
    private final ReferenceDataPort referenceDataPort;

    public void loadAdditives() {
        log.info("Starting additive loading orchestration...");
        List<AdditiveData> additives = additiveSourcePort.fetchAdditives();

        if (additives != null && !additives.isEmpty()) {
            referenceDataPort.getOrCreateAdditives(additives);
            log.info("Successfully processed {} additives from source.", additives.size());
        } else {
            log.warn("No additives found from source.");
        }
    }
}

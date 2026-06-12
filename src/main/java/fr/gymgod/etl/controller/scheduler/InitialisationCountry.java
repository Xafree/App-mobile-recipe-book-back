package fr.gymgod.etl.controller.scheduler;

import fr.gymgod.common.entities.nutrition.Country;
import fr.gymgod.common.domain.nutrition.CountryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import static fr.gymgod.etl.domain.model.DataCountry.COUNTRY_DEFINITIONS;

@Service
@RequiredArgsConstructor
@Slf4j
public class InitialisationCountry {

    private final CountryRepository countryRepository;

    @PostConstruct
    @Transactional
    public void init() {
        log.info("Initializing country data...");

        for (Country def : COUNTRY_DEFINITIONS) {
            Country country = countryRepository.findByNameIgnoreCase(def.getName())
                    .orElseGet(() -> {
                        Country c = new Country();
                        c.setName(def.getName());
                        return c;
                    });

            Set<String> uniqueVariants = new LinkedHashSet<>();

            if (country.getVariants() != null) {
                for (String variant : country.getVariants()) {
                    if (variant != null) {
                        uniqueVariants.add(variant.toLowerCase());
                    }
                }
            }

            if (def.getVariants() != null) {
                uniqueVariants.addAll(def.getVariants());
            }

            country.setVariants(new ArrayList<>(uniqueVariants));

            countryRepository.save(country);
        }

        log.info("Country data initialized: {} countries processed.", COUNTRY_DEFINITIONS.size());
    }
}

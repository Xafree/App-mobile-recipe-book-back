package fr.gymgod.app.country.service;

import fr.gymgod.app.country.domain.port.CountryDataPort;
import fr.gymgod.common.entities.nutrition.Country;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestratorCountry {

    private final CountryDataPort countryDataPort;

    public Page<Country> getAllCountries(Pageable pageable) {
        return countryDataPort.getCountries(pageable);
    }

    public List<Country> getAllCountriesList() {
        return countryDataPort.getAllCountries();
    }

    @Transactional
    public Optional<Country> updateCountry(UUID id, Country countryUpdate) {
        return countryDataPort.getCountryById(id).map(country -> {
            Set<String> uniqueVariants = new LinkedHashSet<>();
            if (countryUpdate.getVariants() != null) {
                countryUpdate.getVariants().forEach(v -> {
                    if (v != null && !v.isBlank()) {
                        uniqueVariants.add(v.trim().toLowerCase());
                    }
                });
            }
            country.setVariants(new ArrayList<>(uniqueVariants));
            return countryDataPort.save(country);
        });
    }

    @Transactional
    public String importCountries(MultipartFile file) throws IOException {
        int updatedCount = 0;
        int createdCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine && line.startsWith("\ufeff")) {
                    line = line.substring(1);
                }

                if (isFirstLine || line.trim().isEmpty() || line.startsWith("Canonical Name")) {
                    isFirstLine = false;
                    continue;
                }

                String[] parts = line.split(";", -1);
                if (parts.length < 1)
                    continue;

                String name = parts[0].trim().toLowerCase();
                if (name.isEmpty())
                    continue;

                Set<String> newVariants = new LinkedHashSet<>();
                for (int i = 1; i < parts.length; i++) {
                    if (!parts[i].trim().isEmpty()) {
                        newVariants.add(parts[i].trim().toLowerCase());
                    }
                }

                Optional<Country> existingOpt = countryDataPort.getCountryByNameIgnoreCase(name);
                Country country;
                if (existingOpt.isPresent()) {
                    country = existingOpt.get();
                    Set<String> currentVariants = new LinkedHashSet<>(country.getVariants());
                    currentVariants.addAll(newVariants);
                    country.setVariants(new ArrayList<>(currentVariants));
                    updatedCount++;
                } else {
                    country = Country.builder()
                            .name(name)
                            .variants(new ArrayList<>(newVariants))
                            .build();
                    createdCount++;
                }
                countryDataPort.save(country);
            }
        }

        return "Import successful. Updated: " + updatedCount + ", Created: " + createdCount;
    }
}

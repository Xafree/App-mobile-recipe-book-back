package fr.gymgod.etl.gateway.refdata;

import fr.gymgod.common.entities.nutrition.Country;
import fr.gymgod.common.domain.nutrition.CountryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class CountryService {
    private final CountryRepository countryRepository;
    private final CommonService commonService;
    private final PlatformTransactionManager transactionManager;
    private final List<String> countryMissing = new ArrayList<>();

    public synchronized Map<String, Country> resolveCountries(Set<String> names) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return template.execute(status -> {
            Map<String, Country> resolvedMap = new HashMap<>();

            for (String rawName : names) {
                Country country = findCountry(rawName);
                if (country != null) {
                    resolvedMap.put(rawName, country);
                }
            }

            return resolvedMap;
        });
    }

    public List<Country> getCountries(String[] columns, int idxCountry, Map<String, Country> batchCache) {
        List<Country> lst = new ArrayList<>();
        String rawCountry = this.commonService.getValue(columns, idxCountry);
        if (rawCountry == null || rawCountry.isEmpty()) {
            return lst;
        }

        List<String> stringList = this.commonService.splitIgnoringParentheses(rawCountry);
        for (String name : stringList) {
            String trimmedName = name.trim();
            if (trimmedName.isEmpty())
                continue;

            Country c = batchCache.get(trimmedName);
            if (c != null) {
                lst.add(c);
            }
        }
        return lst;
    }

    private Country findCountry(String name) {
        if (name == null || name.isEmpty())
            return null;

        name = name.toLowerCase();

        // 1. Recherche exacte par nom (ignore case)
        Optional<Country> byName = countryRepository.findByNameIgnoreCase(name);
        if (byName.isPresent()) {
            return byName.get();
        }

        // 2. Recherche par variante (ignore case)
        // On utilise PageRequest.of(0, 1) pour ne récupérer que le premier résultat
        List<Country> byVariant = countryRepository.findByVariantIgnoreCase(name, PageRequest.of(0, 1));
        if (!byVariant.isEmpty()) {
            return byVariant.get(0);
        }

        // 3. Logique de fallback "en:" et répétitions (ex: "en:en:")
        if (name.startsWith("en:")) {
            String fallback = name;
            while (fallback.startsWith("en:")) {
                fallback = fallback.substring(3);
            }
            fallback = fallback.replace("-", " ");

            if (!fallback.isEmpty()) {
                // On tente de voir si le fallback correspond à un nom ou une variante
                Optional<Country> fallbackByName = countryRepository.findByNameIgnoreCase(fallback);
                if (fallbackByName.isPresent()) {
                    return fallbackByName.get();
                }

                List<Country> fallbackByVariant = countryRepository.findByVariantIgnoreCase(fallback,
                        PageRequest.of(0, 1));
                if (!fallbackByVariant.isEmpty()) {
                    return fallbackByVariant.get(0);
                }
            }
        }

        // 4. Non trouvé -> ajout dans countryMissing et log warning
        if (!countryMissing.contains(name)) {
            countryMissing.add(name);
            log.warn("Pays non trouvé : {}", name);
        }

        return null;
    }

    public List<String> getCountryMissing() {
        return countryMissing;
    }
}

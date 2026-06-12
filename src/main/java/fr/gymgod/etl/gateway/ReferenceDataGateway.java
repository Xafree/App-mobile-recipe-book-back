package fr.gymgod.etl.gateway;

import fr.gymgod.etl.domain.port.ReferenceDataPort;
import fr.gymgod.common.entities.nutrition.*;
import fr.gymgod.common.entities.user.UserAccount;
import fr.gymgod.etl.gateway.refdata.*;
import fr.gymgod.etl.domain.model.AdditiveData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapter that implements the ReferenceDataPort.
 * It acts as a bridge between the core ETL domain and the legacy Spring
 * services/repositories.
 */
@Service
@RequiredArgsConstructor
public class ReferenceDataGateway implements ReferenceDataPort {

    private final BrandService brandService;
    private final CategorieService categorieService;
    private final CountryService countryService;
    private final LabelService labelService;
    private final IngredientService ingredientService;
    private final TraceService traceService;
    private final fr.gymgod.common.domain.nutrition.AdditiveRepository additiveRepository;
    private final Map<String, Additive> additiveCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public Map<String, Brand> resolveBrands(Set<String> names) {
        return brandService.resolveBrands(names);
    }

    @Override
    public Map<String, Categorie> resolveCategories(Set<String> names) {
        return categorieService.resolveCategories(names);
    }

    @Override
    public Map<String, Country> resolveCountries(Set<String> names) {
        return countryService.resolveCountries(names);
    }

    @Override
    public Map<String, Label> resolveLabels(Set<String> names) {
        return labelService.resolveLabels(names);
    }

    @Override
    public Map<String, Ingredient> resolveIngredients(Set<String> names) {
        return ingredientService.resolveIngredients(names);
    }

    @Override
    public Map<String, Trace> resolveTraces(Set<String> names) {
        return traceService.resolveTraces(names);
    }

    @Override
    public List<String> getCountryMissing() {
        return countryService.getCountryMissing();
    }

    @Override
    public List<Ingredient> getOrCreateIngredients(List<String> names) {
        return ingredientService.getIngredients(names);
    }

    @Override
    public List<Additive> getOrCreateAdditives(List<AdditiveData> additives) {
        if (additives == null || additives.isEmpty())
            return new java.util.ArrayList<>();
        List<Additive> result = new java.util.ArrayList<>();
        for (AdditiveData content : additives) {
            String code = content.getCode();
            if (code != null) {
                code = code.trim().toUpperCase();
                if (code.startsWith("E") && code.length() < 10) {
                    result.add(getOrCreateAdditive(code, content.getName(), content.getDangerLevel()));
                }
            }
        }
        return result;
    }

    private Additive getOrCreateAdditive(String code, String name, int dangerLevel) {
        if (additiveCache.containsKey(code)) {
            Additive existing = additiveCache.get(code);
            boolean changed = false;
            if (dangerLevel > 0 && existing.getDangerLevel() != dangerLevel) {
                existing.setDangerLevel(dangerLevel);
                changed = true;
            }
            if (name != null && !name.isEmpty()
                    && (existing.getName() == null || existing.getName().startsWith("Additif "))) {
                existing.setName(name);
                changed = true;
            }
            if (changed) {
                additiveRepository.save(existing);
            }
            return existing;
        }

        return additiveRepository.findByCode(code)
                .map(existing -> {
                    boolean changed = false;
                    if (dangerLevel > 0 && existing.getDangerLevel() != dangerLevel) {
                        existing.setDangerLevel(dangerLevel);
                        changed = true;
                    }
                    if (name != null && !name.isEmpty()
                            && (existing.getName() == null || existing.getName().startsWith("Additif "))) {
                        existing.setName(name);
                        changed = true;
                    }
                    if (changed) {
                        additiveRepository.save(existing);
                    }
                    additiveCache.put(code, existing);
                    return existing;
                })
                .orElseGet(() -> {
                    Additive a = new Additive();
                    a.setCode(code);
                    a.setName((name != null && !name.isEmpty()) ? name : "Additif " + code);
                    a.setDangerLevel(dangerLevel);

                    Additive saved = additiveRepository.save(a);
                    additiveCache.put(code, saved);

                    return saved;
                });
    }
}

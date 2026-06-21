package fr.gymgod.etl.service;

import fr.gymgod.common.entities.nutrition.Country;
import fr.gymgod.common.entities.nutrition.*;
import fr.gymgod.common.entities.user.UserAccount;
import fr.gymgod.etl.domain.port.ReferenceDataPort;
import fr.gymgod.etl.domain.service.EtlDataParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class EtlReferenceCacheService {

    private final ReferenceDataPort referenceDataPort;

    // Global Caches
    private final Map<String, Brand> globalBrandCache = new ConcurrentHashMap<>();
    private final Map<String, Categorie> globalCategorieCache = new ConcurrentHashMap<>();
    private final Map<String, Country> globalCountryCache = new ConcurrentHashMap<>();
    private final Map<String, Label> globalLabelCache = new ConcurrentHashMap<>();
    private final Map<String, Ingredient> globalIngredientCache = new ConcurrentHashMap<>();
    private final Map<String, Trace> globalTraceCache = new ConcurrentHashMap<>();

    // Canonical Caches (ID -> Entity)
    private final Map<java.util.UUID, Brand> brandCanonicalCache = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, Categorie> categorieCanonicalCache = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, Country> countryCanonicalCache = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, Label> labelCanonicalCache = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, Ingredient> ingredientCanonicalCache = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, Trace> traceCanonicalCache = new ConcurrentHashMap<>();

    public Map<String, Brand> resolveBrands(List<String[]> batchColumns) {
        return resolveWithCache(batchColumns, 18, globalBrandCache, brandCanonicalCache,
                referenceDataPort::resolveBrands,
                Collections::singletonList, Brand::getId);
    }

    public Map<String, Categorie> resolveCategories(List<String[]> batchColumns) {
        return resolveWithCache(batchColumns, 21, globalCategorieCache, categorieCanonicalCache,
                referenceDataPort::resolveCategories, EtlDataParser::splitIgnoringParentheses, Categorie::getId);
    }

    public Map<String, Country> resolveCountries(List<String[]> batchColumns) {
        return resolveWithCache(batchColumns, 39, globalCountryCache, countryCanonicalCache,
                referenceDataPort::resolveCountries, EtlDataParser::splitIgnoringParentheses, Country::getId);
    }

    public Map<String, Label> resolveLabels(List<String[]> batchColumns) {
        return resolveWithCache(batchColumns, 29, globalLabelCache, labelCanonicalCache,
                referenceDataPort::resolveLabels,
                EtlDataParser::splitIgnoringParentheses, Label::getId);
    }

    public Map<String, Ingredient> resolveIngredients(List<String[]> batchColumns) {
        return resolveWithCache(batchColumns, 42, globalIngredientCache, ingredientCanonicalCache,
                referenceDataPort::resolveIngredients, EtlDataParser::splitIgnoringParentheses, Ingredient::getId);
    }

    public Map<String, Trace> resolveTraces(List<String[]> batchColumns) {
        return resolveWithCache(batchColumns, 47, globalTraceCache, traceCanonicalCache,
                referenceDataPort::resolveTraces,
                EtlDataParser::splitIgnoringParentheses, Trace::getId);
    }

    private <T> Map<String, T> resolveWithCache(List<String[]> batchColumns, int index,
            Map<String, T> globalCache,
            Map<java.util.UUID, T> canonicalCache,
            Function<Set<String>, Map<String, T>> resolver,
            Function<String, List<String>> keyExtractor,
            Function<T, java.util.UUID> idExtractor) {
        Map<String, T> result = new HashMap<>();
        Set<String> requiredKeys = new HashSet<>();
        boolean allInCache = true;

        for (String[] col : batchColumns) {
            String val = EtlDataParser.getValue(col, index);
            if (val != null && !val.isEmpty()) {
                List<String> keys = keyExtractor.apply(val);
                for (String key : keys) {
                    String trimmedKey = key.trim();
                    if (!trimmedKey.isEmpty()) {
                        requiredKeys.add(trimmedKey);
                        if (!globalCache.containsKey(trimmedKey)) {
                            allInCache = false;
                        }
                    }
                }
            }
        }

        if (allInCache) {
            for (String key : requiredKeys) {
                T item = globalCache.get(key);
                if (item != null) {
                    result.put(key, item);
                }
            }
            return result;
        }

        Map<String, T> batchResult = resolver.apply(requiredKeys);

        if (batchResult != null) {
            for (Map.Entry<String, T> entry : batchResult.entrySet()) {
                String key = entry.getKey();
                T entity = entry.getValue();

                if (entity != null) {
                    java.util.UUID id = idExtractor.apply(entity);
                    T canonical = canonicalCache.computeIfAbsent(id, k -> entity);
                    globalCache.put(key, canonical);
                    result.put(key, canonical);
                }
            }

            for (String key : requiredKeys) {
                if (globalCache.containsKey(key)) {
                    result.put(key, globalCache.get(key));
                }
            }
        }

        return result;
    }
}

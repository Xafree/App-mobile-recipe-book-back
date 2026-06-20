package fr.gymgod.etl.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.gymgod.common.entities.nutrition.Country;
import fr.gymgod.common.entities.nutrition.*;
import fr.gymgod.etl.domain.port.ReferenceDataPort;
import fr.gymgod.etl.domain.service.EtlDataParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

/**
 * Caches de résolution des entités de référence (marques, catégories, pays,
 * labels, ingrédients, traces) pendant l'import ETL produits.
 *
 * <p>Bornés à {@link #MAX_CACHE_ENTRIES} entrées chacun (LRU via Caffeine) :
 * sur le dump OpenFoodFacts complet, les ingrédients/labels en texte libre
 * génèrent des centaines de milliers de clés uniques — un cache illimité
 * (ancien {@code ConcurrentHashMap}, jamais vidé sur la durée de vie du bean
 * singleton) finissait par épuiser le tas JVM (OutOfMemoryError observé en
 * cours d'import).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EtlReferenceCacheService {

    private static final int MAX_CACHE_ENTRIES = 50_000;

    private final ReferenceDataPort referenceDataPort;

    // Caches globaux (clé texte -> entité), bornés LRU.
    private final Cache<String, Brand> globalBrandCache = boundedCache();
    private final Cache<String, Categorie> globalCategorieCache = boundedCache();
    private final Cache<String, Country> globalCountryCache = boundedCache();
    private final Cache<String, Label> globalLabelCache = boundedCache();
    private final Cache<String, Ingredient> globalIngredientCache = boundedCache();
    private final Cache<String, Trace> globalTraceCache = boundedCache();

    // Caches canoniques (ID -> entité), bornés LRU.
    private final Cache<UUID, Brand> brandCanonicalCache = boundedCache();
    private final Cache<UUID, Categorie> categorieCanonicalCache = boundedCache();
    private final Cache<UUID, Country> countryCanonicalCache = boundedCache();
    private final Cache<UUID, Label> labelCanonicalCache = boundedCache();
    private final Cache<UUID, Ingredient> ingredientCanonicalCache = boundedCache();
    private final Cache<UUID, Trace> traceCanonicalCache = boundedCache();

    private static <K, V> Cache<K, V> boundedCache() {
        return Caffeine.newBuilder().maximumSize(MAX_CACHE_ENTRIES).build();
    }

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
            Cache<String, T> globalCache,
            Cache<UUID, T> canonicalCache,
            Function<Set<String>, Map<String, T>> resolver,
            Function<String, List<String>> keyExtractor,
            Function<T, UUID> idExtractor) {
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
                        if (globalCache.getIfPresent(trimmedKey) == null) {
                            allInCache = false;
                        }
                    }
                }
            }
        }

        if (allInCache) {
            for (String key : requiredKeys) {
                T item = globalCache.getIfPresent(key);
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
                    UUID id = idExtractor.apply(entity);
                    T canonical = canonicalCache.get(id, k -> entity);
                    globalCache.put(key, canonical);
                    result.put(key, canonical);
                }
            }

            for (String key : requiredKeys) {
                T cached = globalCache.getIfPresent(key);
                if (cached != null) {
                    result.put(key, cached);
                }
            }
        }

        return result;
    }
}

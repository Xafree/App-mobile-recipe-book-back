package fr.gymgod.etl.gateway.refdata;

import fr.gymgod.common.entities.nutrition.Ingredient;
import fr.gymgod.common.domain.nutrition.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IngredientService {
    private final IngredientRepository ingredientRepository;
    private final CommonService commonService;
    private final PlatformTransactionManager transactionManager;

    public synchronized Map<String, Ingredient> resolveIngredients(Set<String> names) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return template.execute(status -> {
            if (names.isEmpty()) {
                return Collections.emptyMap();
            }

            List<Ingredient> existing = ingredientRepository.findByNameIn(names);
            Map<String, Ingredient> ingredientMap = existing.stream()
                    .collect(Collectors.toMap(Ingredient::getName, i -> i));

            List<Ingredient> newIngredients = new ArrayList<>();
            for (String name : names) {
                if (!ingredientMap.containsKey(name)) {
                    Ingredient i = new Ingredient();
                    i.setName(name);
                    newIngredients.add(i);
                    ingredientMap.put(name, i);
                }
            }

            if (!newIngredients.isEmpty()) {
                ingredientRepository.saveAll(newIngredients);
            }

            return ingredientMap;
        });
    }

    public List<Ingredient> getIngredients(List<String> names) {
        if (names == null || names.isEmpty()) return new ArrayList<>();

        List<Ingredient> ingredients = new ArrayList<>();
        for (String name : names) {
            if (name == null) continue;
            String cleanName = name.trim();
            if (!cleanName.isEmpty()) {
                // Check cache or DB
                // Note: For simplicity in this AI flow, we might lose the "global cache" benefit within the batch 
                // unless we pass the cache. But given AI slowness, direct DB hits or local cache interactions are acceptable.
                // Or better: use the repository directly or a cached method.
                // Assuming we don't have the cache map here, we rely on repository (L1 cache of Hibernate might help).
                ingredients.add(resolveIngredient(cleanName));
            }
        }
        return ingredients;
    }

    private Ingredient resolveIngredient(String name) {
        String cleanName = name.trim();
        // 1. Try to find existing
        List<Ingredient> existing = ingredientRepository.findByNameIgnoreCase(cleanName);
        if (!existing.isEmpty()) {
            return existing.get(0); // Pick the first one if duplicates exist
        }

        // 2. If not found, try to create
        try {
            Ingredient i = new Ingredient();
            i.setName(cleanName);
            return ingredientRepository.save(i);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Concurrent insertion happened? Retry find
            List<Ingredient> retried = ingredientRepository.findByNameIgnoreCase(cleanName);
            if (!retried.isEmpty()) {
                return retried.get(0);
            }
            throw new IllegalStateException("Failed to resolve ingredient even after concurrency retry: " + cleanName);
        }
    }

    public List<Ingredient> getIngredients(String[] columns, int index, Map<String, Ingredient> cache) {
        List<Ingredient> lst = new ArrayList<>();
        String rawIngredients = this.commonService.getValue(columns, index);
        if (rawIngredients == null || rawIngredients.isEmpty()) {
            return lst;
        }

        List<String> stringList = this.commonService.splitIgnoringParentheses(rawIngredients);
        for (String name : stringList) {
            name = name.trim();
            if (name.isEmpty()) continue;
            Ingredient i = cache.get(name);
            if (i != null) {
                lst.add(i);
            }
        }
        return lst;
    }
}

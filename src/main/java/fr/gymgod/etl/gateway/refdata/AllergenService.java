package fr.gymgod.etl.gateway.refdata;

import fr.gymgod.common.entities.nutrition.Allergen;
import fr.gymgod.common.domain.nutrition.AllergenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AllergenService {

    private final AllergenRepository allergenRepository;
    private final CommonService commonService;
    private final PlatformTransactionManager transactionManager;

    public synchronized Map<String, Allergen> resolveAllergens(Set<String> names) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return template.execute(status -> {
            if (names.isEmpty()) {
                return Collections.emptyMap();
            }

            List<Allergen> existing = allergenRepository.findByNameIn(names);
            Map<String, Allergen> allergenMap = existing.stream()
                    .collect(Collectors.toMap(Allergen::getName, a -> a));

            List<Allergen> newAllergens = new ArrayList<>();
            for (String name : names) {
                if (!allergenMap.containsKey(name)) {
                    Allergen a = new Allergen();
                    a.setName(name);
                    newAllergens.add(a);
                    allergenMap.put(name, a);
                }
            }

            if (!newAllergens.isEmpty()) {
                allergenRepository.saveAll(newAllergens);
            }

            return allergenMap;
        });
    }

    public List<Allergen> getAllergens(String[] columns, int idxAllergen, Map<String, Allergen> batchCache) {
        List<Allergen> lst = new ArrayList<>();
        String rawAllergens = this.commonService.getValue(columns, idxAllergen);
        if (rawAllergens == null || rawAllergens.isEmpty()) {
            return lst;
        }

        List<String> stringList = this.commonService.splitIgnoringParentheses(rawAllergens);
        for (String name : stringList) {
            name = name.trim();
            if (name.isEmpty()) continue;
            Allergen a = batchCache.get(name);
            if (a != null) {
                lst.add(a);
            }
        }
        return lst;
    }
}

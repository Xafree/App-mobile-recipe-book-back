package fr.gymgod.etl.gateway.refdata;

import fr.gymgod.common.entities.nutrition.Categorie;
import fr.gymgod.common.domain.nutrition.CategorieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategorieService {
    private final CategorieRepository categorieRepository;
    private final CommonService commonService;
    private final PlatformTransactionManager transactionManager;

    public synchronized Map<String, Categorie> resolveCategories(Set<String> names) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return template.execute(status -> {
            if (names.isEmpty()) {
                return Collections.emptyMap();
            }

            List<Categorie> existing = categorieRepository.findByNameIn(names);
            Map<String, Categorie> categorieMap = existing.stream()
                    .collect(Collectors.toMap(Categorie::getName, c -> c));

            List<Categorie> newCategories = new ArrayList<>();
            for (String name : names) {
                if (!categorieMap.containsKey(name)) {
                    Categorie c = new Categorie();
                    c.setName(name);
                    newCategories.add(c);
                    categorieMap.put(name, c);
                }
            }

            if (!newCategories.isEmpty()) {
                categorieRepository.saveAll(newCategories);
            }

            return categorieMap;
        });
    }

    public List<Categorie> getCategories(String[] columns, int idxCategories, Map<String, Categorie> batchCache) {
        List<Categorie> lst = new ArrayList<>();
        String rawCategories = this.commonService.getValue(columns, idxCategories);
        if (rawCategories == null || rawCategories.isEmpty()) {
            return lst;
        }

        List<String> stringList = this.commonService.splitIgnoringParentheses(rawCategories);
        for (String name : stringList) {
            name = name.trim();
            if (name.isEmpty()) continue;
            Categorie c = batchCache.get(name);
            if (c != null) {
                lst.add(c);
            }
        }
        return lst;
    }
}

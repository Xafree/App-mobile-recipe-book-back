package fr.gymgod.etl.gateway.refdata;

import fr.gymgod.common.entities.nutrition.Brand;
import fr.gymgod.common.domain.nutrition.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BrandService {
    private final BrandRepository brandRepository;
    private final CommonService commonService;
    private final PlatformTransactionManager transactionManager;

    public synchronized Map<String, Brand> resolveBrands(Set<String> names) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return template.execute(status -> {
            if (names.isEmpty()) {
                return Collections.emptyMap();
            }

            List<Brand> existing = brandRepository.findByNameIn(names);
            Map<String, Brand> brandMap = existing.stream()
                    .collect(Collectors.toMap(Brand::getName, b -> b));

            List<Brand> newBrands = new ArrayList<>();
            for (String name : names) {
                if (!brandMap.containsKey(name)) {
                    Brand b = new Brand();
                    b.setName(name);
                    newBrands.add(b);
                    brandMap.put(name, b);
                }
            }

            if (!newBrands.isEmpty()) {
                brandRepository.saveAll(newBrands);
            }

            return brandMap;
        });
    }

    public Brand getBrand(String[] columns, int idxBrand, Map<String, Brand> batchCache) {
        String name = this.commonService.getValue(columns, idxBrand);
        if (name == null) return null;
        return batchCache.get(name);
    }
}

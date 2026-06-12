package fr.gymgod.etl.gateway.refdata;

import fr.gymgod.common.entities.nutrition.Label;
import fr.gymgod.common.domain.nutrition.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabelService {
    private final LabelRepository labelRepository;
    private final CommonService commonService;
    private final PlatformTransactionManager transactionManager;

    public synchronized Map<String, Label> resolveLabels(Set<String> names) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return template.execute(status -> {
            if (names.isEmpty()) {
                return Collections.emptyMap();
            }

            List<Label> existing = labelRepository.findByNameIn(names);
            Map<String, Label> labelMap = existing.stream()
                    .collect(Collectors.toMap(Label::getName, l -> l));

            List<Label> newLabels = new ArrayList<>();
            for (String name : names) {
                if (!labelMap.containsKey(name)) {
                    Label l = new Label();
                    l.setName(name);
                    newLabels.add(l);
                    labelMap.put(name, l);
                }
            }

            if (!newLabels.isEmpty()) {
                labelRepository.saveAll(newLabels);
            }

            return labelMap;
        });
    }

    public List<Label> getLabels(String[] columns, int idxLabels, Map<String, Label> batchCache) {
        List<Label> lst = new ArrayList<>();
        String rawLabels = this.commonService.getValue(columns, idxLabels);
        if (rawLabels == null || rawLabels.isEmpty()) {
            return lst;
        }

        List<String> stringList = this.commonService.splitIgnoringParentheses(rawLabels);
        for (String name : stringList) {
            name = name.trim();
            if (name.isEmpty()) continue;
            Label l = batchCache.get(name);
            if (l != null) {
                lst.add(l);
            }
        }
        return lst;
    }
}

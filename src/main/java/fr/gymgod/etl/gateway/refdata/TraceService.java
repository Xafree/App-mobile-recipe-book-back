package fr.gymgod.etl.gateway.refdata;

import fr.gymgod.common.entities.nutrition.Trace;
import fr.gymgod.common.domain.nutrition.TraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TraceService {
    private final TraceRepository traceRepository;
    private final CommonService commonService;
    private final PlatformTransactionManager transactionManager;

    public synchronized Map<String, Trace> resolveTraces(Set<String> names) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return template.execute(status -> {
            if (names.isEmpty()) {
                return Collections.emptyMap();
            }

            List<Trace> existing = traceRepository.findByNameIn(names);
            Map<String, Trace> traceMap = existing.stream()
                    .collect(Collectors.toMap(Trace::getName, t -> t));

            List<Trace> newTraces = new ArrayList<>();
            for (String name : names) {
                if (!traceMap.containsKey(name)) {
                    Trace t = new Trace();
                    t.setName(name);
                    newTraces.add(t);
                    traceMap.put(name, t);
                }
            }

            if (!newTraces.isEmpty()) {
                traceRepository.saveAll(newTraces);
            }

            return traceMap;
        });
    }

    public List<Trace> getTraces(String[] columns, int idxTraces, Map<String, Trace> batchCache) {
        List<Trace> lst = new ArrayList<>();
        String rawTraces = this.commonService.getValue(columns, idxTraces);
        if (rawTraces == null || rawTraces.isEmpty()) {
            return lst;
        }

        List<String> stringList = this.commonService.splitIgnoringParentheses(rawTraces);
        for (String name : stringList) {
            name = name.trim();
            if (name.isEmpty()) continue;
            Trace t = batchCache.get(name);
            if (t != null) {
                lst.add(t);
            }
        }
        return lst;
    }
}

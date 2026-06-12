package fr.gymgod.app.nutrition.domain.mapper;

import fr.gymgod.app.nutrition.domain.entites.record.TraceRecord;
import fr.gymgod.common.entities.nutrition.Trace;
import org.springframework.stereotype.Service;

@Service
public class TraceTransform {

    public TraceRecord fromTrace(Trace trace) {
        return new TraceRecord(trace.getId(), trace.getName());
    }
}

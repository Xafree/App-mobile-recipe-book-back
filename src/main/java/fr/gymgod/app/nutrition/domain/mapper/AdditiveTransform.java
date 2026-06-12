package fr.gymgod.app.nutrition.domain.mapper;

import fr.gymgod.app.nutrition.domain.entites.record.AdditiveRecord;
import fr.gymgod.common.entities.nutrition.Additive;
import org.springframework.stereotype.Service;

@Service
public class AdditiveTransform {

    public AdditiveRecord fromAdditive(Additive additive) {
        if (additive == null) {
            return null;
        }
        return new AdditiveRecord(
                additive.getCode(),
                additive.getName(),
                additive.getDangerLevel()
        );
    }
}

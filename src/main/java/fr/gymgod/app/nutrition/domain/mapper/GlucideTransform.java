package fr.gymgod.app.nutrition.domain.mapper;

import fr.gymgod.app.nutrition.domain.entites.record.GlucideRecord;
import fr.gymgod.common.entities.nutrition.Glucide;
import org.springframework.stereotype.Service;

@Service
public class GlucideTransform {

    public GlucideRecord fromGlucide(Glucide glucide) {
        return new GlucideRecord(
                glucide.getId(),
                glucide.getSodium100g(),
                glucide.getSalt100g(),
                glucide.getPotassium100g(),
                glucide.getMagnesium100g(),
                glucide.getCalcium100g()
        );
    }
}

package fr.gymgod.app.nutrition.domain.mapper;

import fr.gymgod.app.nutrition.domain.entites.record.NutrimentRecord;
import fr.gymgod.common.entities.nutrition.Nutriment;
import org.springframework.stereotype.Service;

@Service
public class NutrimentTransform {

    public NutrimentRecord fromNutriment(Nutriment nutriment) {
        return new NutrimentRecord(
                nutriment.getId(),
                nutriment.getEnergyKcal100g(),
                nutriment.getProteins100g(),
                nutriment.getCarbohydrates100g(),
                nutriment.getFat100g(),
                nutriment.getFiber100g(),
                nutriment.getSugars100g(),
                nutriment.getSaturatedFat100g()
        );
    }
}

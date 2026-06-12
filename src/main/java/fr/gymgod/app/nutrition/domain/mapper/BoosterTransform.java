package fr.gymgod.app.nutrition.domain.mapper;

import fr.gymgod.app.nutrition.domain.entites.record.BoosterRecord;
import fr.gymgod.common.entities.nutrition.Booster;
import org.springframework.stereotype.Service;

@Service
public class BoosterTransform {

    public BoosterRecord fromBooster(Booster booster) {
        return new BoosterRecord(
                booster.getId(),
                booster.getIron100g(),
                booster.getCaffeine100g(),
                booster.getTaurine100g(),
                booster.getCarnitine100g(),
                booster.getNutritionScoreFr100g(),
                booster.getAlcohol100g()
        );
    }
}

package fr.gymgod.app.nutrition.domain.mapper;

import fr.gymgod.app.nutrition.domain.entites.record.VitaminRecord;
import fr.gymgod.common.entities.nutrition.Vitamin;
import org.springframework.stereotype.Service;

@Service
public class VitaminTransform {

    public VitaminRecord fromVitamin(Vitamin vitamin) {
        return new VitaminRecord(
                vitamin.getId(),
                vitamin.getA100g(),
                vitamin.getD100g(),
                vitamin.getE100g(),
                vitamin.getK100g(),
                vitamin.getC100g(),
                vitamin.getB1100g(),
                vitamin.getB2100g(),
                vitamin.getPp100g(),
                vitamin.getB6100g(),
                vitamin.getB9100g(),
                vitamin.getB12100g()
        );
    }
}

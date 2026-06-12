package fr.gymgod.app.nutrition.domain.mapper;

import fr.gymgod.app.nutrition.domain.entites.record.AllergenRecord;
import fr.gymgod.common.entities.nutrition.Allergen;
import org.springframework.stereotype.Service;

@Service
public class AllergenTransform {

    public AllergenRecord fromAllergen(Allergen allergen) {
        return new AllergenRecord(allergen.getId(), allergen.getName());
    }
}

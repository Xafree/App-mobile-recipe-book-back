package fr.gymgod.app.nutrition.domain.mapper;

import fr.gymgod.app.nutrition.domain.entites.record.CountryRecord;
import fr.gymgod.common.entities.nutrition.Country;
import org.springframework.stereotype.Service;

@Service
public class CountryTransform {

    public CountryRecord fromCountry(Country country) {
        return new CountryRecord(country.getId(), country.getName());
    }
}

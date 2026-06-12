package fr.gymgod.app.nutrition.domain.mapper;

import fr.gymgod.app.nutrition.domain.entites.record.BrandRecord;
import fr.gymgod.common.entities.nutrition.Brand;
import org.springframework.stereotype.Service;

@Service
public class BrandTransform {

    public BrandRecord fromBrand(Brand brand) {
        return new BrandRecord(brand.getId(), brand.getName());
    }
}

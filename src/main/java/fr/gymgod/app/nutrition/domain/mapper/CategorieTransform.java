package fr.gymgod.app.nutrition.domain.mapper;

import fr.gymgod.app.nutrition.domain.entites.record.CategorieRecord;
import fr.gymgod.common.entities.nutrition.Categorie;
import org.springframework.stereotype.Service;

@Service
public class CategorieTransform {

    public CategorieRecord fromCategorie(Categorie categorie) {
        return new CategorieRecord(categorie.getId(), categorie.getName());
    }
}

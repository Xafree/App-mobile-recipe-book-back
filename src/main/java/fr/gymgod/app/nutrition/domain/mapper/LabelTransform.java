package fr.gymgod.app.nutrition.domain.mapper;

import fr.gymgod.app.nutrition.domain.entites.record.LabelRecord;
import fr.gymgod.common.entities.nutrition.Label;
import org.springframework.stereotype.Service;

@Service
public class LabelTransform {

    public LabelRecord fromLabel(Label label) {
        return new LabelRecord(label.getId(), label.getName());
    }
}

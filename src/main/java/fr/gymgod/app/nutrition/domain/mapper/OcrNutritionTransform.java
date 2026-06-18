package fr.gymgod.app.nutrition.domain.mapper;

import fr.gymgod.app.nutrition.domain.entites.record.OcrNutritionRecord;
import fr.gymgod.etl.domain.model.OcrNutritionData;
import org.springframework.stereotype.Service;

/**
 * Conversion entre {@link OcrNutritionData} (domaine ETL, rempli par un
 * {@link fr.gymgod.etl.domain.port.OcrLabelParsingPort}) et
 * {@link OcrNutritionRecord} (DTO exposé par l'API nutrition).
 */
@Service
public class OcrNutritionTransform {

    public OcrNutritionRecord toRecord(OcrNutritionData data) {
        return new OcrNutritionRecord(
                data.servingSizeG(),
                data.energyKcalServing(),
                data.proteinsServing(),
                data.carbohydratesServing(),
                data.fatServing(),
                data.sugarsServing(),
                data.saturatedFatServing(),
                data.transFatServing(),
                data.fiberServing(),
                data.cholesterolServing(),
                data.sodiumServing(),
                data.potassiumServing(),
                data.calciumServing(),
                data.ironServing(),
                data.preparedServingSizeG(),
                data.energyKcalPreparedServing(),
                data.proteinsPreparedServing(),
                data.carbohydratesPreparedServing(),
                data.fatPreparedServing(),
                data.sugarsPreparedServing(),
                data.saturatedFatPreparedServing(),
                data.transFatPreparedServing(),
                data.fiberPreparedServing(),
                data.sodiumPreparedServing());
    }

    /** Réponse vide (tous champs {@code null}) — le client retombe sur un formulaire vierge. */
    public OcrNutritionRecord empty() {
        return new OcrNutritionRecord(null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}

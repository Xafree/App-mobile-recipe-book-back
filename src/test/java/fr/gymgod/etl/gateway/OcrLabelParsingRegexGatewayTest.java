package fr.gymgod.etl.gateway;

import fr.gymgod.etl.domain.model.OcrNutritionData;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OcrLabelParsingRegexGatewayTest {

    private final OcrLabelParsingRegexGateway gateway = new OcrLabelParsingRegexGateway();

    @Test
    void rawTextNullOuVideRenvoieEmpty() {
        assertThat(gateway.parseLabel(null)).isEmpty();
        assertThat(gateway.parseLabel("  ")).isEmpty();
    }

    @Test
    void etiquetteFrMonoColonne() {
        String rawText = """
                Pour 100 g
                Energie 380 kcal / 1590 kJ
                Protéines 8 g
                Glucides 70 g
                dont sucres 25 g
                Lipides 5 g
                dont acides gras saturés 1.5 g
                Fibres 3 g
                Sel 0.5 g
                """;

        OcrNutritionData data = gateway.parseLabel(rawText).orElseThrow();

        assertThat(data.servingSizeG()).isEqualTo(100.0);
        assertThat(data.energyKcalServing()).isEqualTo(380.0);
        assertThat(data.proteinsServing()).isEqualTo(8.0);
        assertThat(data.carbohydratesServing()).isEqualTo(70.0);
        assertThat(data.sugarsServing()).isEqualTo(25.0);
        assertThat(data.fatServing()).isEqualTo(5.0);
        assertThat(data.saturatedFatServing()).isEqualTo(1.5);
        assertThat(data.fiberServing()).isEqualTo(3.0);
        // sel 0.5 g -> sodium = 0.5 * 1000 / 2.5 = 200 mg
        assertThat(data.sodiumServing()).isEqualTo(200.0);
        assertThat(data.preparedServingSizeG()).isNull();
        assertThat(data.energyKcalPreparedServing()).isNull();
    }

    @Test
    void etiquetteEnMonoColonne() {
        String rawText = """
                Per 2/3 cup (55g)
                Calories 230
                Fat 4 g
                Saturated Fat 1 g
                Trans Fat 0 g
                Carbohydrate 37 g
                Fibre 4 g
                Sugars 1 g
                Protein 3 g
                Cholesterol 5 mg
                Sodium 160 mg
                Potassium 250 mg
                Calcium 20 mg
                Iron 0.3 mg
                """;

        OcrNutritionData data = gateway.parseLabel(rawText).orElseThrow();

        assertThat(data.servingSizeG()).isEqualTo(55.0);
        assertThat(data.energyKcalServing()).isEqualTo(230.0);
        assertThat(data.fatServing()).isEqualTo(4.0);
        assertThat(data.saturatedFatServing()).isEqualTo(1.0);
        assertThat(data.transFatServing()).isEqualTo(0.0);
        assertThat(data.carbohydratesServing()).isEqualTo(37.0);
        assertThat(data.fiberServing()).isEqualTo(4.0);
        assertThat(data.sugarsServing()).isEqualTo(1.0);
        assertThat(data.proteinsServing()).isEqualTo(3.0);
        assertThat(data.cholesterolServing()).isEqualTo(5.0);
        assertThat(data.sodiumServing()).isEqualTo(160.0);
        assertThat(data.potassiumServing()).isEqualTo(250.0);
        assertThat(data.calciumServing()).isEqualTo(20.0);
        assertThat(data.ironServing()).isEqualTo(0.3);
        assertThat(data.preparedServingSizeG()).isNull();
    }

    @Test
    void etiquetteDeuxColonnesAsPrepared() {
        String rawText = """
                Pour 100 g / 250 ml préparé
                Energie 380 kcal / 180 kcal
                Protéines 8 g / 7 g
                Glucides 70 g / 30 g
                dont sucres 25 g / 20 g
                Lipides 5 g / 4.5 g
                """;

        OcrNutritionData data = gateway.parseLabel(rawText).orElseThrow();

        assertThat(data.servingSizeG()).isEqualTo(100.0);
        assertThat(data.energyKcalServing()).isEqualTo(380.0);
        assertThat(data.proteinsServing()).isEqualTo(8.0);
        assertThat(data.carbohydratesServing()).isEqualTo(70.0);
        assertThat(data.sugarsServing()).isEqualTo(25.0);
        assertThat(data.fatServing()).isEqualTo(5.0);

        assertThat(data.preparedServingSizeG()).isEqualTo(250.0);
        assertThat(data.energyKcalPreparedServing()).isEqualTo(180.0);
        assertThat(data.proteinsPreparedServing()).isEqualTo(7.0);
        assertThat(data.carbohydratesPreparedServing()).isEqualTo(30.0);
        assertThat(data.sugarsPreparedServing()).isEqualTo(20.0);
        assertThat(data.fatPreparedServing()).isEqualTo(4.5);
    }

    @Test
    void phraseDePortionPreparéeRenseignePreparedServingSizeSansDeclencherLesAutresChampsPrepared() {
        String rawText = """
                Per 1/4 cup dry (28 g) / 140 g prepared
                Calories 110
                Fat 2.5 g
                Saturated 1.5 g
                Trans 0 g
                Carbohydrate 20 g
                Fibre 1 g
                Sugars 1 g
                Protein 2 g
                Sodium 450 mg
                """;

        OcrNutritionData data = gateway.parseLabel(rawText).orElseThrow();

        assertThat(data.servingSizeG()).isEqualTo(28.0);
        assertThat(data.energyKcalServing()).isEqualTo(110.0);
        assertThat(data.sodiumServing()).isEqualTo(450.0);
        // Phrase "... / 140 g prepared" → portion préparée renseignée même
        // sans seconde colonne de valeurs nutritionnelles.
        assertThat(data.preparedServingSizeG()).isEqualTo(140.0);
        assertThat(data.energyKcalPreparedServing()).isNull();
        assertThat(data.proteinsPreparedServing()).isNull();
    }

    @Test
    void aucunePhraseDePortionPreparéeNeRenseignePreparedServingSize() {
        String rawText = """
                Per 1/4 cup dry (28 g) / 140 g cooked
                Calories 110
                Fat 2.5 g
                """;

        OcrNutritionData data = gateway.parseLabel(rawText).orElseThrow();

        assertThat(data.servingSizeG()).isEqualTo(28.0);
        assertThat(data.preparedServingSizeG()).isNull();
    }

    @Test
    void conversionKjVersKcalSeule() {
        String rawText = """
                Pour 100 g
                Energie 1672 kJ
                Protéines 8 g
                """;

        OcrNutritionData data = gateway.parseLabel(rawText).orElseThrow();

        // 1672 / 4.184 ≈ 399.6
        assertThat(data.energyKcalServing()).isEqualTo(1672 / 4.184);
    }

    @Test
    void texteSansLigneReconnueRenvoieChampsNulls() {
        String rawText = "Ingrédients: eau, sucre, sel.\nA conserver au frais.";

        Optional<OcrNutritionData> result = gateway.parseLabel(rawText);

        assertThat(result).isPresent();
        OcrNutritionData data = result.get();
        assertThat(data.servingSizeG()).isEqualTo(100.0);
        assertThat(data.energyKcalServing()).isNull();
        assertThat(data.proteinsServing()).isNull();
        assertThat(data.sodiumServing()).isNull();
    }
}

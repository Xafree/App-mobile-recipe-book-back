package fr.gymgod.app.nutrition.domain.entites.record;

import java.util.List;

public record ProductRecord(
        String code,
        String url,
        String createdTime,
        String lastModifiedTime,
        String productName,
        String quantity,
        BrandRecord brand,
        List<CategorieRecord> categorie,
        List<CountryRecord> country,
        List<LabelRecord> label,
        List<IngredientRecord> ingredient,
        List<AllergenRecord> allergen,
        List<TraceRecord> trace,
        List<AdditiveRecord> additives, // [NEW]
        int nutriscore,
        String nutriscoreGrade,
        NutrimentRecord nutriment,
        GlucideRecord glucide,
        VitaminRecord vitamin,
        ImagesRecord image
) {
}

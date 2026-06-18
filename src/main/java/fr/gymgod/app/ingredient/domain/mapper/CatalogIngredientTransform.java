package fr.gymgod.app.ingredient.domain.mapper;

import fr.gymgod.app.ingredient.domain.entites.record.CatalogIngredientCreateRecord;
import fr.gymgod.app.ingredient.domain.entites.record.CatalogIngredientRecord;
import fr.gymgod.app.recipe.domain.entites.record.NutritionSnapshot;
import fr.gymgod.common.entities.nutrition.CatalogIngredient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogIngredientTransform {

    /** Applique [request] sur [ingredient] (création ou mise à jour par upsert). */
    public void applyCreateRecord(CatalogIngredient ingredient, CatalogIngredientCreateRecord request) {
        NutritionSnapshot nutrition = request.nutrition();
        ingredient.setName(request.name().trim());
        ingredient.setImageUrl(request.imageUrl());
        ingredient.setUnit(request.unit());
        ingredient.setCaloriesPer100g(nutrition.caloriesPer100g());
        ingredient.setProteinPer100g(nutrition.proteinPer100g());
        ingredient.setCarbsPer100g(nutrition.carbsPer100g());
        ingredient.setFatPer100g(nutrition.fatPer100g());
        ingredient.setFiberPer100g(nutrition.fiberPer100g());
        ingredient.setSugarPer100g(nutrition.sugarPer100g());
        ingredient.setSaturatedFatPer100g(nutrition.saturatedFatPer100g());
        ingredient.setTransFatPer100g(nutrition.transFatPer100g());
        ingredient.setCholesterolPer100g(nutrition.cholesterolPer100g());
        ingredient.setSodiumPer100g(nutrition.sodiumPer100g());
        ingredient.setPotassiumPer100g(nutrition.potassiumPer100g());
        ingredient.setCalciumPer100g(nutrition.calciumPer100g());
        ingredient.setIronPer100g(nutrition.ironPer100g());
        ingredient.setPreparedRatio(nutrition.preparedRatio());
        ingredient.setCaloriesPreparedPer100g(nutrition.caloriesPreparedPer100g());
        ingredient.setProteinPreparedPer100g(nutrition.proteinPreparedPer100g());
        ingredient.setCarbsPreparedPer100g(nutrition.carbsPreparedPer100g());
        ingredient.setFatPreparedPer100g(nutrition.fatPreparedPer100g());
        ingredient.setExternalFoodCode(request.externalFoodCode());
        ingredient.setExternalProductSnapshot(request.externalProductSnapshot());
    }

    public CatalogIngredientRecord fromEntity(CatalogIngredient ingredient) {
        NutritionSnapshot nutrition = new NutritionSnapshot(
                ingredient.getCaloriesPer100g(),
                ingredient.getProteinPer100g(),
                ingredient.getCarbsPer100g(),
                ingredient.getFatPer100g(),
                ingredient.getFiberPer100g(),
                ingredient.getSugarPer100g(),
                ingredient.getSaturatedFatPer100g(),
                ingredient.getTransFatPer100g(),
                ingredient.getCholesterolPer100g(),
                ingredient.getSodiumPer100g(),
                ingredient.getPotassiumPer100g(),
                ingredient.getCalciumPer100g(),
                ingredient.getIronPer100g(),
                ingredient.getPreparedRatio(),
                ingredient.getCaloriesPreparedPer100g(),
                ingredient.getProteinPreparedPer100g(),
                ingredient.getCarbsPreparedPer100g(),
                ingredient.getFatPreparedPer100g());
        return new CatalogIngredientRecord(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getImageUrl(),
                ingredient.getUnit(),
                nutrition,
                ingredient.getExternalFoodCode(),
                ingredient.getExternalProductSnapshot());
    }

    public List<CatalogIngredientRecord> fromEntities(List<CatalogIngredient> ingredients) {
        return ingredients.stream().map(this::fromEntity).toList();
    }
}

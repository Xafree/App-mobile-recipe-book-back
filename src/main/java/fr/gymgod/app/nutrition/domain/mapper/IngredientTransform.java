package fr.gymgod.app.nutrition.domain.mapper;

import fr.gymgod.app.nutrition.domain.entites.record.IngredientRecord;
import fr.gymgod.common.entities.nutrition.Ingredient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IngredientTransform {

    public Ingredient fromIngredientRecord(IngredientRecord ingredientRecord) {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(ingredientRecord.id());
        ingredient.setName(ingredientRecord.name());
        return ingredient;
    }

    public List<Ingredient> fromIngredientRecord(List<IngredientRecord> ingredientRecords) {
        return ingredientRecords.stream()
                .map(this::fromIngredientRecord)
                .toList();
    }

    public IngredientRecord fromIngredient(Ingredient ingredient) {
        return new IngredientRecord(ingredient.getId(), ingredient.getName());
    }

    public List<IngredientRecord> fromIngredients(List<Ingredient> ingredients) {
        return ingredients.stream()
                .map(this::fromIngredient)
                .toList();
    }
}

package fr.gymgod.app.meal.domain.mapper;

import fr.gymgod.app.meal.domain.entites.record.MealItemRecord;
import fr.gymgod.app.meal.domain.entites.record.MealRecord;
import fr.gymgod.app.nutrition.domain.mapper.ProductTransform;
import fr.gymgod.common.entities.nutrition.Meal;
import fr.gymgod.common.entities.nutrition.MealItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealTransform {

    private final ProductTransform productTransform;

    public MealRecord fromMeal(Meal meal) {
        if (meal == null) {
            return null;
        }

        List<MealItemRecord> itemRecords = meal.getItems().stream()
                .map(this::fromMealItem)
                .collect(Collectors.toList());

        return new MealRecord(
                meal.getId(),
                meal.getType(),
                meal.getDate(),
                itemRecords
        );
    }

    public MealItemRecord fromMealItem(MealItem item) {
        if (item == null) {
            return null;
        }
        return new MealItemRecord(
                item.getId(),
                productTransform.fromProduct(item.getProduct()),
                item.getQuantity()
        );
    }
}

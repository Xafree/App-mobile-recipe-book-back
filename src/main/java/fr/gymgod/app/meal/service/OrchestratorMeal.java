package fr.gymgod.app.meal.service;

import fr.gymgod.app.meal.domain.port.MealDataPort;
import fr.gymgod.app.meal.domain.entites.record.MealCreateRecord;
import fr.gymgod.app.meal.domain.entites.record.MealItemCreateRecord;
import fr.gymgod.app.meal.domain.entites.record.MealRecord;
import fr.gymgod.app.meal.domain.mapper.MealTransform;
import fr.gymgod.common.entities.nutrition.Meal;
import fr.gymgod.common.entities.nutrition.MealItem;
import fr.gymgod.common.entities.nutrition.Product;
import fr.gymgod.common.entities.user.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrchestratorMeal {

    private final MealDataPort mealDataPort;
    private final MealTransform mealTransform;

    public List<MealRecord> getUserMeals() {
        UserAccount user = mealDataPort.getCurrentUser();
        List<Meal> meals = mealDataPort.findMealsByUser(user);
        return meals.stream()
                .map(mealTransform::fromMeal)
                .collect(Collectors.toList());
    }

    public MealRecord getMeal(UUID id) {
        UserAccount currentUser = mealDataPort.getCurrentUser();
        return mealDataPort.findMealById(id).map(meal -> {
            if (!meal.getUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Accès refusé : ce repas ne vous appartient pas.");
            }
            return mealTransform.fromMeal(meal);
        }).orElse(null);
    }

    public List<MealRecord> getMealsByDate(java.time.LocalDate date) {
        UserAccount user = mealDataPort.getCurrentUser();
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        List<Meal> meals = mealDataPort.findMealsByUserAndDateBetween(user, start, end);
        return meals.stream()
                .map(mealTransform::fromMeal)
                .collect(Collectors.toList());
    }

    @Transactional
    public MealRecord createMeal(MealCreateRecord request) {
        UserAccount user = mealDataPort.getCurrentUser();

        Meal meal = new Meal();
        meal.setUserId(user.getId());
        meal.setType(request.type());
        meal.setDate(request.date() != null ? request.date() : LocalDateTime.now());

        for (MealItemCreateRecord itemRequest : request.items()) {
            Product product = mealDataPort.getProduct(itemRequest.productId());
            if (product != null) {
                MealItem item = new MealItem();
                item.setProduct(product);
                item.setQuantity(itemRequest.quantity());
                meal.addItem(item);
            }
        }

        if (request.recipeId() != null) {
            fr.gymgod.common.entities.nutrition.Recipe recipe = mealDataPort.getRecipe(request.recipeId());
            if (recipe != null) {
                for (fr.gymgod.common.entities.nutrition.RecipeItem recipeItem : recipe.getIngredients()) {
                    if (recipeItem.getExternalFoodCode() == null) continue;
                    Product product = mealDataPort.getProduct(recipeItem.getExternalFoodCode());
                    if (product == null) continue;
                    MealItem item = new MealItem();
                    item.setProduct(product);
                    item.setQuantity(recipeItem.getQuantity().doubleValue());
                    meal.addItem(item);
                }
            }
        }

        Meal savedMeal = mealDataPort.saveMeal(meal);
        return mealTransform.fromMeal(savedMeal);
    }

    @Transactional
    public MealRecord updateMeal(UUID id, MealCreateRecord request) {
        UserAccount currentUser = mealDataPort.getCurrentUser();

        return mealDataPort.findMealById(id).map(meal -> {
            if (!meal.getUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Accès refusé : ce repas ne vous appartient pas.");
            }

            meal.setType(request.type());
            if (request.date() != null) {
                meal.setDate(request.date());
            }

            meal.getItems().clear();
            for (MealItemCreateRecord itemRequest : request.items()) {
                Product product = mealDataPort.getProduct(itemRequest.productId());
                if (product != null) {
                    MealItem item = new MealItem();
                    item.setProduct(product);
                    item.setQuantity(itemRequest.quantity());
                    meal.addItem(item);
                }
            }

            if (request.recipeId() != null) {
                fr.gymgod.common.entities.nutrition.Recipe recipe = mealDataPort.getRecipe(request.recipeId());
                if (recipe != null) {
                    for (fr.gymgod.common.entities.nutrition.RecipeItem recipeItem : recipe.getIngredients()) {
                        if (recipeItem.getExternalFoodCode() == null) continue;
                        Product product = mealDataPort.getProduct(recipeItem.getExternalFoodCode());
                        if (product == null) continue;
                        MealItem item = new MealItem();
                        item.setProduct(product);
                        item.setQuantity(recipeItem.getQuantity().doubleValue());
                        meal.addItem(item);
                    }
                }
            }

            Meal savedMeal = mealDataPort.saveMeal(meal);
            return mealTransform.fromMeal(savedMeal);
        }).orElse(null);
    }

    @Transactional
    public void deleteMeal(UUID id) {
        UserAccount currentUser = mealDataPort.getCurrentUser();
        mealDataPort.findMealById(id).ifPresent(meal -> {
            if (!meal.getUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Accès refusé : ce repas ne vous appartient pas.");
            }
            mealDataPort.deleteMeal(id);
        });
    }
}

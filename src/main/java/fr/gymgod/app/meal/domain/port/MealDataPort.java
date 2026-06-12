package fr.gymgod.app.meal.domain.port;

import fr.gymgod.common.entities.nutrition.Meal;
import fr.gymgod.common.entities.nutrition.Product;
import fr.gymgod.common.entities.user.UserAccount;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealDataPort {
    UserAccount getCurrentUser();

    List<Meal> findMealsByUser(UserAccount user);

    Optional<Meal> findMealById(UUID id);

    List<Meal> findMealsByUserAndDateBetween(UserAccount user, LocalDateTime start, LocalDateTime end);

    Product getProduct(String productId);

    fr.gymgod.common.entities.nutrition.Recipe getRecipe(UUID recipeId);

    Meal saveMeal(Meal meal);

    void deleteMeal(UUID id);
}

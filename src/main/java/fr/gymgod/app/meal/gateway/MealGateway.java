package fr.gymgod.app.meal.gateway;

import fr.gymgod.app.meal.domain.port.MealDataPort;
import fr.gymgod.common.domain.nutrition.MealRepository;
import fr.gymgod.common.domain.nutrition.ProductRepository;
import fr.gymgod.common.entities.nutrition.Meal;
import fr.gymgod.common.entities.nutrition.Product;
import fr.gymgod.common.entities.user.UserAccount;
import fr.gymgod.common.service.SessionSpringService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MealGateway implements MealDataPort {

    private final MealRepository mealRepository;
    private final ProductRepository productRepository;
    private final fr.gymgod.common.domain.nutrition.RecipeRepository recipeRepository;
    private final SessionSpringService sessionSpringService;

    @Override
    public UserAccount getCurrentUser() {
        return sessionSpringService.getCurrentUser();
    }

    @Override
    public List<Meal> findMealsByUser(UserAccount user) {
        return mealRepository.findByUserId(user.getId());
    }

    @Override
    public Optional<Meal> findMealById(UUID id) {
        return mealRepository.findById(id);
    }

    @Override
    public List<Meal> findMealsByUserAndDateBetween(UserAccount user, LocalDateTime start, LocalDateTime end) {
        return mealRepository.findByUserIdAndDateBetween(user.getId(), start, end);
    }

    @Override
    public Product getProduct(String productId) {
        return productRepository.findById(productId).orElse(null);
    }

    @Override
    public fr.gymgod.common.entities.nutrition.Recipe getRecipe(UUID recipeId) {
        return recipeRepository.findById(recipeId).orElse(null);
    }

    @Override
    public Meal saveMeal(Meal meal) {
        return mealRepository.save(meal);
    }

    @Override
    public void deleteMeal(UUID id) {
        mealRepository.deleteById(id);
    }
}

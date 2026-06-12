package fr.gymgod.app.recipe.gateway;

import fr.gymgod.app.recipe.domain.port.RecipeDataPort;
import fr.gymgod.common.domain.nutrition.RecipeRepository;
import fr.gymgod.common.entities.nutrition.Recipe;
import fr.gymgod.common.entities.user.UserAccount;
import fr.gymgod.common.service.SessionSpringService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeGateway implements RecipeDataPort {

    private final RecipeRepository recipeRepository;
    private final SessionSpringService sessionSpringService;

    @Override
    public UserAccount getCurrentUser() {
        return sessionSpringService.getCurrentUser();
    }

    @Override
    public List<Recipe> findRecipesByUser(UserAccount user) {
        return recipeRepository.findByUserId(user.getId());
    }

    @Override
    public List<Recipe> findFavoriteRecipesByUser(UserAccount user) {
        return recipeRepository.findByUserIdAndIsFavoriteTrue(user.getId());
    }

    @Override
    public Optional<Recipe> findRecipeById(UUID id) {
        return recipeRepository.findById(id);
    }

    @Override
    public Recipe saveRecipe(Recipe recipe) {
        return recipeRepository.save(recipe);
    }

    @Override
    public void deleteRecipe(UUID id) {
        recipeRepository.deleteById(id);
    }
}

package fr.gymgod.app.recipe.domain.port;

import fr.gymgod.common.entities.nutrition.Recipe;
import fr.gymgod.common.entities.user.UserAccount;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeDataPort {
    UserAccount getCurrentUser();

    List<Recipe> findRecipesByUser(UserAccount user);

    List<Recipe> findFavoriteRecipesByUser(UserAccount user);

    Optional<Recipe> findRecipeById(UUID id);

    Recipe saveRecipe(Recipe recipe);

    void deleteRecipe(UUID id);
}

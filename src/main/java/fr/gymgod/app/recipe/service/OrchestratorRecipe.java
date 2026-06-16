package fr.gymgod.app.recipe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gymgod.app.recipe.domain.entites.record.RecipeCreateRecord;
import fr.gymgod.app.recipe.domain.entites.record.RecipeItemCreateRecord;
import fr.gymgod.app.recipe.domain.entites.record.RecipeRecord;
import fr.gymgod.app.recipe.domain.mapper.RecipeTransform;
import fr.gymgod.app.recipe.domain.port.RecipeDataPort;
import fr.gymgod.common.constants.RecipeConstants;
import fr.gymgod.common.domain.user.UserAccountRepository;
import fr.gymgod.common.entities.nutrition.Recipe;
import fr.gymgod.common.entities.nutrition.RecipeItem;
import fr.gymgod.common.entities.user.UserAccount;
import fr.gymgod.common.exception.ExternalProductSnapshotTooLargeException;
import fr.gymgod.common.exception.RecipeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrchestratorRecipe {

    private final RecipeDataPort recipeDataPort;
    private final RecipeTransform recipeTransform;
    private final UserAccountRepository userAccountRepository;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public List<RecipeRecord> getUserRecipes() {
        UserAccount currentUser = recipeDataPort.getCurrentUser();
        return recipeDataPort.findRecipesByUser(currentUser).stream()
                .map(recipeTransform::fromRecipe)
                .collect(Collectors.toList());
    }

    public List<RecipeRecord> getUserFavoriteRecipes() {
        UserAccount currentUser = recipeDataPort.getCurrentUser();
        return recipeDataPort.findFavoriteRecipesByUser(currentUser).stream()
                .map(recipeTransform::fromRecipe)
                .collect(Collectors.toList());
    }

    public RecipeRecord getRecipe(UUID id) {
        UserAccount currentUser = recipeDataPort.getCurrentUser();
        Recipe recipe = recipeDataPort.findRecipeById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
        if (!recipe.getUserId().equals(currentUser.getId())) {
            throw new RuntimeException("Accès refusé : cette recette ne vous appartient pas.");
        }
        return recipeTransform.fromRecipe(recipe);
    }

    /**
     * Retourne le détail complet d'une recette publique sans vérification d'ownership.
     * Retourne une erreur 404 si la recette n'existe pas ou n'est pas publique,
     * afin de ne pas exposer l'existence des recettes privées.
     */
    public RecipeRecord getPublicRecipe(UUID id) {
        Recipe recipe = recipeDataPort.findRecipeById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
        if (!Boolean.TRUE.equals(recipe.getIsPublic())) {
            throw new RecipeNotFoundException(id);
        }
        String authorName = resolveDisplayName(recipe.getUserId());
        return recipeTransform.fromRecipe(recipe, authorName);
    }

    /** Résout le nom d'affichage d'un utilisateur à partir de son UUID. */
    private String resolveDisplayName(UUID userId) {
        return userAccountRepository.findById(userId)
                .map(u -> {
                    String first = u.getFirstName();
                    String last = u.getLastName();
                    if (first != null && !first.isBlank()) {
                        return last != null && !last.isBlank()
                                ? first + " " + last
                                : first;
                    }
                    return u.getUsername() != null ? u.getUsername() : "Utilisateur";
                })
                .orElse("Utilisateur");
    }

    @Transactional
    public RecipeRecord createRecipe(RecipeCreateRecord request) {
        UserAccount currentUser = recipeDataPort.getCurrentUser();

        Recipe recipe = buildRecipe(request, currentUser.getId());
        addIngredients(recipe, request.ingredients());
        applyMacros(recipe, request.ingredients(), request.servings());

        return recipeTransform.fromRecipe(recipeDataPort.saveRecipe(recipe));
    }

    @Transactional
    public RecipeRecord updateRecipe(UUID id, RecipeCreateRecord request) {
        UserAccount currentUser = recipeDataPort.getCurrentUser();
        Recipe recipe = recipeDataPort.findRecipeById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));

        if (!recipe.getUserId().equals(currentUser.getId())) {
            throw new RuntimeException("Accès refusé : cette recette ne vous appartient pas.");
        }

        recipe.setTitle(request.title());
        recipe.setDescription(request.description());
        recipe.setCategory(request.category());
        recipe.setImageUrl(request.imageUrl());
        recipe.setServings(request.servings());
        recipe.setPrepTimeMinutes(request.prepTimeMinutes());
        recipe.setIsPublic(request.isPublic());
        if (request.isFavorite() != null) recipe.setIsFavorite(request.isFavorite());

        recipe.setSteps(request.steps());
        recipe.getIngredients().clear();
        addIngredients(recipe, request.ingredients());
        applyMacros(recipe, request.ingredients(), request.servings());

        return recipeTransform.fromRecipe(recipeDataPort.saveRecipe(recipe));
    }

    @Transactional
    public void deleteRecipe(UUID id) {
        UserAccount currentUser = recipeDataPort.getCurrentUser();
        Recipe recipe = recipeDataPort.findRecipeById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
        if (!recipe.getUserId().equals(currentUser.getId())) {
            throw new RuntimeException("Accès refusé : cette recette ne vous appartient pas.");
        }
        recipeDataPort.deleteRecipe(id);
    }

    private Recipe buildRecipe(RecipeCreateRecord req, UUID userId) {
        Recipe recipe = new Recipe();
        recipe.setUserId(userId);
        recipe.setTitle(req.title());
        recipe.setDescription(req.description());
        recipe.setCategory(req.category());
        recipe.setImageUrl(req.imageUrl());
        recipe.setServings(req.servings());
        recipe.setPrepTimeMinutes(req.prepTimeMinutes());
        recipe.setIsPublic(req.isPublic() != null ? req.isPublic() : false);
        recipe.setIsFavorite(req.isFavorite() != null ? req.isFavorite() : false);
        recipe.setSteps(req.steps());
        return recipe;
    }

    private void addIngredients(Recipe recipe, List<RecipeItemCreateRecord> payloads) {
        for (int i = 0; i < payloads.size(); i++) {
            RecipeItemCreateRecord payload = payloads.get(i);
            validateSnapshot(payload);
            RecipeItem item = new RecipeItem();
            item.setPosition(i + 1);
            item.setName(payload.name());
            item.setImageUrl(payload.imageUrl());
            item.setQuantity(payload.quantity());
            item.setUnit(payload.unit());
            item.setCaloriesPer100g(payload.nutrition().caloriesPer100g());
            item.setProteinPer100g(payload.nutrition().proteinPer100g());
            item.setCarbsPer100g(payload.nutrition().carbsPer100g());
            item.setFatPer100g(payload.nutrition().fatPer100g());
            item.setFiberPer100g(payload.nutrition().fiberPer100g());
            item.setSugarPer100g(payload.nutrition().sugarPer100g());
            item.setSaturatedFatPer100g(payload.nutrition().saturatedFatPer100g());
            item.setTransFatPer100g(payload.nutrition().transFatPer100g());
            item.setSodiumPer100g(payload.nutrition().sodiumPer100g());
            item.setExternalFoodCode(payload.externalFoodCode());
            item.setExternalProductSnapshot(payload.externalProductSnapshot());
            recipe.addIngredient(item);
        }
    }

    private void applyMacros(Recipe recipe, List<RecipeItemCreateRecord> payloads, int servings) {
        RecipeMacroCalculator.AggregatedMacros macros =
                RecipeMacroCalculator.aggregate(payloads, servings);
        recipe.setCaloriesPerServing(macros.caloriesPerServing());
        recipe.setProteinPerServing(macros.proteinPerServing());
        recipe.setCarbsPerServing(macros.carbsPerServing());
        recipe.setFatPerServing(macros.fatPerServing());
    }

    private void validateSnapshot(RecipeItemCreateRecord payload) {
        if (payload.externalProductSnapshot() == null) return;
        try {
            int length = OBJECT_MAPPER.writeValueAsString(payload.externalProductSnapshot()).length();
            if (length > RecipeConstants.MAX_EXTERNAL_PRODUCT_SNAPSHOT_LENGTH) {
                throw new ExternalProductSnapshotTooLargeException(payload.name(), length);
            }
        } catch (ExternalProductSnapshotTooLargeException e) {
            throw e;
        } catch (Exception ignored) {
        }
    }
}

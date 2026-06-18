package fr.gymgod.common.domain.nutrition;

import fr.gymgod.common.entities.nutrition.UserLikedRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserLikedRecipeRepository extends JpaRepository<UserLikedRecipe, UUID> {

    boolean existsByUserIdAndRecipeId(UUID userId, UUID recipeId);

    void deleteByUserIdAndRecipeId(UUID userId, UUID recipeId);

    /** Toutes les lignes aimées par un utilisateur — pour charger la liste "Favoris". */
    List<UserLikedRecipe> findAllByUserId(UUID userId);

    /** Retourne toutes les lignes userId/recipeId pour une liste de recettes — batch check. */
    List<UserLikedRecipe> findAllByUserIdAndRecipeIdIn(UUID userId, List<UUID> recipeIds);
}

package fr.gymgod.app.user.service;

import fr.gymgod.app.feed.domain.record.RecipeSummaryRecord;
import fr.gymgod.app.user.domain.record.PublicProfileRecord;
import fr.gymgod.common.domain.nutrition.RecipeRepository;
import fr.gymgod.common.domain.user.UserAccountRepository;
import fr.gymgod.common.entities.nutrition.Recipe;
import fr.gymgod.common.entities.user.UserAccount;
import fr.gymgod.common.exception.RecipeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service pour les profils publics — recettes publiques d'un utilisateur tiers.
 */
@Service
@RequiredArgsConstructor
public class PublicUserService {

    private final UserAccountRepository userAccountRepository;
    private final RecipeRepository recipeRepository;

    @Transactional(readOnly = true)
    public PublicProfileRecord getPublicProfile(UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RecipeNotFoundException(userId));

        String displayName = buildDisplayName(user);

        List<Recipe> recipes = recipeRepository
                .findByUserIdAndIsPublicTrueOrderByCreatedAtDesc(userId);

        List<RecipeSummaryRecord> summaries = recipes.stream()
                .map(r -> RecipeSummaryRecord.from(r, displayName, false))
                .toList();

        return new PublicProfileRecord(
                userId,
                displayName,
                user.getAvatarUrl(),
                summaries.size(),
                summaries
        );
    }

    private String buildDisplayName(UserAccount user) {
        String first = user.getFirstName();
        String last  = user.getLastName();
        if (first != null && !first.isBlank()) {
            return last != null && !last.isBlank() ? first + " " + last : first;
        }
        return user.getUsername() != null ? user.getUsername() : "Utilisateur";
    }
}

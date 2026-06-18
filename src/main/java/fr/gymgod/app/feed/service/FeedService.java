package fr.gymgod.app.feed.service;

import fr.gymgod.app.feed.domain.record.FeedPageRecord;
import fr.gymgod.app.feed.domain.record.RecipeSummaryRecord;
import fr.gymgod.common.constants.FeedConstants;
import fr.gymgod.common.domain.nutrition.RecipeRepository;
import fr.gymgod.common.domain.nutrition.UserLikedRecipeRepository;
import fr.gymgod.common.domain.user.UserAccountRepository;
import fr.gymgod.common.entities.nutrition.Recipe;
import fr.gymgod.common.entities.nutrition.UserLikedRecipe;
import fr.gymgod.common.entities.user.UserAccount;
import fr.gymgod.common.service.SessionSpringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service du fil public des recettes.
 * Utilise la pagination par curseur (keyset) pour rester performant
 * à grande échelle sans OFFSET.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private final RecipeRepository recipeRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserLikedRecipeRepository userLikedRecipeRepository;
    private final SessionSpringService sessionSpringService;

    @Transactional(readOnly = true)
    public FeedPageRecord getPublicFeed(String category, int size, String cursor) {
        int clampedSize = Math.min(Math.max(size, 1), FeedConstants.MAX_PAGE_SIZE);
        String normalizedCategory = (category == null || category.isBlank()) ? null : category.strip();

        UUID currentUserId = resolveCurrentUserId();

        // Fetch clampedSize + 1 pour détecter hasMore sans COUNT(*)
        List<Recipe> fetched = fetchPage(normalizedCategory, cursor, clampedSize + 1, currentUserId);

        boolean hasMore = fetched.size() > clampedSize;
        List<Recipe> pageRecipes = hasMore ? fetched.subList(0, clampedSize) : fetched;

        Map<UUID, String> authorNames = resolveAuthorNames(pageRecipes);
        Set<UUID> likedIds = resolveLikedIds(pageRecipes, currentUserId);
        List<RecipeSummaryRecord> records = pageRecipes.stream()
                .map(r -> RecipeSummaryRecord.from(
                        r,
                        authorNames.getOrDefault(r.getUserId(), "Utilisateur"),
                        likedIds.contains(r.getId()),
                        currentUserId != null && r.getUserId().equals(currentUserId)))
                .toList();

        String nextCursor = hasMore ? encodeCursor(pageRecipes.getLast()) : null;

        log.debug("Feed: size={}, category={}, hasCursor={}, hasMore={}",
                clampedSize, normalizedCategory, cursor != null, hasMore);

        return new FeedPageRecord(records, hasMore, nextCursor);
    }

    private List<Recipe> fetchPage(String category, String cursor, int fetchLimit, UUID excludeUserId) {
        PageRequest limit = PageRequest.of(0, fetchLimit);
        if (cursor == null || cursor.isBlank()) {
            return recipeRepository.findPublicFeedFirstPage(category, limit);
        }
        long[] parts = decodeCursor(cursor);
        Instant timestamp = Instant.ofEpochMilli(parts[0]);
        UUID id = new UUID(parts[1], parts[2]);
        return recipeRepository.findPublicFeedNextPage(category, timestamp, id, limit);
    }

    private UUID resolveCurrentUserId() {
        try {
            return UUID.fromString(sessionSpringService.getCurrentUserId());
        } catch (Exception e) {
            return null;
        }
    }

    /** Batch-vérification des likes de l'utilisateur courant sur une page de recettes. */
    private Set<UUID> resolveLikedIds(List<Recipe> recipes, UUID currentUserId) {
        if (currentUserId == null) return new HashSet<>();
        List<UUID> recipeIds = recipes.stream().map(Recipe::getId).collect(Collectors.toList());
        return userLikedRecipeRepository.findAllByUserIdAndRecipeIdIn(currentUserId, recipeIds)
                .stream()
                .map(UserLikedRecipe::getRecipeId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /** Batch-résolution des noms d'auteurs pour éviter N+1 queries. */
    private Map<UUID, String> resolveAuthorNames(List<Recipe> recipes) {
        List<UUID> userIds = recipes.stream()
                .map(Recipe::getUserId)
                .distinct()
                .collect(Collectors.toList());
        return userAccountRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        UserAccount::getId,
                        u -> {
                            String first = u.getFirstName();
                            String last  = u.getLastName();
                            if (first != null && !first.isBlank()) {
                                return last != null && !last.isBlank()
                                        ? first + " " + last : first;
                            }
                            return u.getUsername() != null ? u.getUsername() : "Utilisateur";
                        }
                ));
    }

    private String encodeCursor(Recipe last) {
        String raw = last.getCreatedAt().toEpochMilli()
                + FeedConstants.CURSOR_SEPARATOR + last.getId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Décode le curseur et retourne [epochMilli, uuidMostSig, uuidLeastSig].
     */
    private long[] decodeCursor(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\" + FeedConstants.CURSOR_SEPARATOR, 2);
            if (parts.length != 2) throw new IllegalArgumentException("Invalid cursor format");
            long epochMilli = Long.parseLong(parts[0]);
            UUID uuid = UUID.fromString(parts[1]);
            return new long[]{epochMilli, uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()};
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Malformed pagination cursor", e);
        }
    }
}

package fr.gymgod.app.feed.domain.record;

import java.util.List;

/**
 * Page du fil public des recettes avec curseur opaque pour la pagination suivante.
 */
public record FeedPageRecord(
        List<RecipeSummaryRecord> recipes,
        boolean hasMore,
        String nextCursor
) {}

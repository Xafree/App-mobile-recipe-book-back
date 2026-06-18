package fr.gymgod.app.feed.controller;

import fr.gymgod.app.feed.domain.record.FeedPageRecord;
import fr.gymgod.app.feed.service.FeedService;
import fr.gymgod.common.constants.FeedConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * Fil public des recettes — toutes les recettes publiques de tous les utilisateurs,
 * les plus récentes en premier, avec pagination par curseur opaque.
 */
@RestController
@RequestMapping("/v1/feed")
@RequiredArgsConstructor
public class FeedControllerAdapter {

    private final FeedService feedService;

    @GetMapping("/recipes")
    public ResponseEntity<FeedPageRecord> getPublicFeed(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "" + FeedConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(required = false) String cursor) {

        FeedPageRecord page = feedService.getPublicFeed(category, size, cursor);

        // Feed personnalisé (exclut les recettes du user courant) → pas de cache partagé.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(page);
    }
}

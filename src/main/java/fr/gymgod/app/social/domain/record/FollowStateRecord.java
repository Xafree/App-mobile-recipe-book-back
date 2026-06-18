package fr.gymgod.app.social.domain.record;

import java.util.UUID;

/**
 * État du suivi entre l'utilisateur courant et un autre utilisateur.
 *
 * @param userId     UUID de l'utilisateur cible (pour faciliter la désérialisation côté client)
 * @param iFollowThem l'utilisateur courant suit la cible
 * @param theyFollowMe la cible suit l'utilisateur courant
 * @param isMutual   les deux se suivent mutuellement
 */
public record FollowStateRecord(
    UUID userId,
    boolean iFollowThem,
    boolean theyFollowMe,
    boolean isMutual
) {}

package fr.gymgod.app.social.service;

import fr.gymgod.app.social.domain.record.FollowStateRecord;
import fr.gymgod.app.social.domain.record.FriendSummaryRecord;
import fr.gymgod.app.social.domain.record.NotificationEvent;
import fr.gymgod.common.domain.social.UserFollowRepository;
import fr.gymgod.common.domain.user.UserAccountRepository;
import fr.gymgod.common.entities.social.UserFollow;
import fr.gymgod.common.entities.user.UserAccount;
import fr.gymgod.common.service.SessionSpringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowService {

    private final UserFollowRepository followRepository;
    private final UserAccountRepository userAccountRepository;
    private final SessionSpringService sessionSpringService;
    private final SseEmitterRegistry sseRegistry;

    @Transactional(transactionManager = "userTransactionManager")
    public FollowStateRecord follow(UUID targetUserId) {
        UUID currentUserId = currentUserId();
        log.info("[follow] currentUserId={} targetUserId={}", currentUserId, targetUserId);
        if (currentUserId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot follow yourself");
        }
        if (!userAccountRepository.existsById(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        boolean alreadyExists = followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);
        log.info("[follow] alreadyExists={}", alreadyExists);
        if (!alreadyExists) {
            UserFollow follow = new UserFollow();
            follow.setFollowerId(currentUserId);
            follow.setFollowingId(targetUserId);
            UserFollow saved = followRepository.save(follow);
            log.info("[follow] saved id={}", saved.getId());
        }
        FollowStateRecord state = buildState(currentUserId, targetUserId);
        log.info("[follow] resulting state={}", state);
        // Notifier la cible : quelqu'un la suit
        userAccountRepository.findById(currentUserId).ifPresent(follower -> {
            String name = resolveDisplayName(follower);
            sseRegistry.push(targetUserId, NotificationEvent.newFollow(currentUserId, name));
            // Si c'est devenu mutuel, notifier également l'initiateur
            if (state.isMutual()) {
                userAccountRepository.findById(targetUserId).ifPresent(target ->
                    sseRegistry.push(currentUserId, NotificationEvent.mutualFollow(
                        targetUserId, resolveDisplayName(target)))
                );
            }
        });
        return state;
    }

    @Transactional(transactionManager = "userTransactionManager")
    public FollowStateRecord unfollow(UUID targetUserId) {
        UUID currentUserId = currentUserId();
        boolean existed = followRepository.findByFollowerIdAndFollowingId(currentUserId, targetUserId)
            .map(f -> {
                followRepository.delete(f);
                return true;
            })
            .orElse(false);
        FollowStateRecord state = buildState(currentUserId, targetUserId);
        log.info("[unfollow] currentUserId={} targetUserId={} existed={} resultingState={}",
                currentUserId, targetUserId, existed, state);
        return state;
    }

    @Transactional(readOnly = true, transactionManager = "userTransactionManager")
    public FollowStateRecord getState(UUID targetUserId) {
        return buildState(currentUserId(), targetUserId);
    }

    /** Liste des amis (suivi mutuel) de l'utilisateur courant, pour les sélecteurs de contact. */
    @Transactional(readOnly = true, transactionManager = "userTransactionManager")
    public List<FriendSummaryRecord> listFriends() {
        List<UUID> friendIds = followRepository.findMutualFollowIds(currentUserId());
        return summariesFor(friendIds);
    }

    /** Liste des utilisateurs qui suivent l'utilisateur courant. */
    @Transactional(readOnly = true, transactionManager = "userTransactionManager")
    public List<FriendSummaryRecord> listFollowers() {
        return summariesFor(followRepository.findFollowerIdsByFollowingId(currentUserId()));
    }

    /** Liste des utilisateurs que l'utilisateur courant suit. */
    @Transactional(readOnly = true, transactionManager = "userTransactionManager")
    public List<FriendSummaryRecord> listFollowing() {
        return summariesFor(followRepository.findFollowingIdsByFollowerId(currentUserId()));
    }

    private List<FriendSummaryRecord> summariesFor(List<UUID> userIds) {
        return userAccountRepository.findAllById(userIds).stream()
            .map(u -> new FriendSummaryRecord(u.getId(), resolveDisplayName(u), u.getAvatarUrl()))
            .toList();
    }

    private FollowStateRecord buildState(UUID currentUserId, UUID targetUserId) {
        boolean iFollowThem = followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);
        boolean theyFollowMe = followRepository.existsByFollowerIdAndFollowingId(targetUserId, currentUserId);
        return new FollowStateRecord(targetUserId, iFollowThem, theyFollowMe, iFollowThem && theyFollowMe);
    }

    private UUID currentUserId() {
        return UUID.fromString(sessionSpringService.getCurrentUserId());
    }

    static String resolveDisplayName(UserAccount user) {
        String first = user.getFirstName();
        String last  = user.getLastName();
        if (first != null && !first.isBlank()) {
            return last != null && !last.isBlank() ? first + " " + last : first;
        }
        return user.getUsername() != null ? user.getUsername() : "Utilisateur";
    }
}

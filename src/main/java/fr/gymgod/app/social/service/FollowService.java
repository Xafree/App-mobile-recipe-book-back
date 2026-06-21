package fr.gymgod.app.social.service;

import fr.gymgod.app.social.domain.record.FollowStateRecord;
import fr.gymgod.app.social.domain.record.NotificationEvent;
import fr.gymgod.common.domain.social.UserFollowRepository;
import fr.gymgod.common.domain.user.UserAccountRepository;
import fr.gymgod.common.entities.social.UserFollow;
import fr.gymgod.common.entities.user.UserAccount;
import fr.gymgod.common.service.SessionSpringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final UserFollowRepository followRepository;
    private final UserAccountRepository userAccountRepository;
    private final SessionSpringService sessionSpringService;
    private final SseEmitterRegistry sseRegistry;

    @Transactional(transactionManager = "userTransactionManager")
    public FollowStateRecord follow(UUID targetUserId) {
        UUID currentUserId = currentUserId();
        if (currentUserId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot follow yourself");
        }
        if (!userAccountRepository.existsById(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        if (!followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)) {
            UserFollow follow = new UserFollow();
            follow.setFollowerId(currentUserId);
            follow.setFollowingId(targetUserId);
            followRepository.save(follow);
        }
        FollowStateRecord state = buildState(currentUserId, targetUserId);
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
        followRepository.findByFollowerIdAndFollowingId(currentUserId, targetUserId)
            .ifPresent(followRepository::delete);
        return buildState(currentUserId, targetUserId);
    }

    @Transactional(readOnly = true, transactionManager = "userTransactionManager")
    public FollowStateRecord getState(UUID targetUserId) {
        return buildState(currentUserId(), targetUserId);
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

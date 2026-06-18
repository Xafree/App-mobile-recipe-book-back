package fr.gymgod.common.domain.social;

import fr.gymgod.common.entities.social.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, UUID> {

    Optional<UserFollow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    /** IDs de tous les utilisateurs que userId suit. */
    @Query("SELECT f.followingId FROM UserFollow f WHERE f.followerId = :userId")
    List<UUID> findFollowingIdsByFollowerId(@Param("userId") UUID userId);

    /** IDs de tous les utilisateurs qui suivent userId. */
    @Query("SELECT f.followerId FROM UserFollow f WHERE f.followingId = :userId")
    List<UUID> findFollowerIdsByFollowingId(@Param("userId") UUID userId);

    /**
     * IDs des utilisateurs avec lesquels userId a un suivi mutuel
     * (chacun suit l'autre).
     */
    @Query("""
        SELECT f1.followingId FROM UserFollow f1
        WHERE f1.followerId = :userId
        AND EXISTS (
            SELECT 1 FROM UserFollow f2
            WHERE f2.followerId = f1.followingId
            AND f2.followingId = :userId
        )
        """)
    List<UUID> findMutualFollowIds(@Param("userId") UUID userId);
}

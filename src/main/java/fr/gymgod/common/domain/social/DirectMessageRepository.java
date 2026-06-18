package fr.gymgod.common.domain.social;

import fr.gymgod.common.entities.social.DirectMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    /** Messages entre deux utilisateurs, du plus récent au plus ancien. */
    @Query("""
        SELECT m FROM DirectMessage m
        WHERE (m.senderId = :userA AND m.receiverId = :userB)
           OR (m.senderId = :userB AND m.receiverId = :userA)
        ORDER BY m.sentAt DESC
        """)
    List<DirectMessage> findConversation(
        @Param("userA") UUID userA,
        @Param("userB") UUID userB,
        Pageable pageable
    );

    /**
     * Tous les messages impliquant userId, du plus récent au plus ancien.
     * Utilisé pour construire la liste des conversations en groupant par partenaire.
     */
    @Query("""
        SELECT m FROM DirectMessage m
        WHERE m.senderId = :userId OR m.receiverId = :userId
        ORDER BY m.sentAt DESC
        """)
    List<DirectMessage> findAllInvolving(@Param("userId") UUID userId);
}

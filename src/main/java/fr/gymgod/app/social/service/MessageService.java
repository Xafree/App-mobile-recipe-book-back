package fr.gymgod.app.social.service;

import fr.gymgod.app.social.domain.record.ConversationRecord;
import fr.gymgod.app.social.domain.record.MessageRecord;
import fr.gymgod.app.social.domain.record.NotificationEvent;
import fr.gymgod.common.domain.social.DirectMessageRepository;
import fr.gymgod.common.domain.social.UserFollowRepository;
import fr.gymgod.common.domain.user.UserAccountRepository;
import fr.gymgod.common.entities.social.DirectMessage;
import fr.gymgod.common.entities.user.UserAccount;
import fr.gymgod.common.service.SessionSpringService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final DirectMessageRepository messageRepository;
    private final UserFollowRepository followRepository;
    private final UserAccountRepository userAccountRepository;
    private final SessionSpringService sessionSpringService;
    private final SseEmitterRegistry sseRegistry;

    @Transactional(transactionManager = "userTransactionManager")
    public MessageRecord sendMessage(UUID receiverId, String content) {
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content cannot be empty");
        }
        UUID senderId = currentUserId();
        // Vérification du follow mutuel
        boolean mutual = followRepository.existsByFollowerIdAndFollowingId(senderId, receiverId)
            && followRepository.existsByFollowerIdAndFollowingId(receiverId, senderId);
        if (!mutual) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Mutual follow required to send messages");
        }
        DirectMessage msg = new DirectMessage();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content.strip());
        // @CreationTimestamp est posé au flush, pas au save() — initialiser manuellement
        // pour que sentAt soit non-null dans le MessageRecord retourné dans la même transaction.
        msg.setSentAt(Instant.now());
        DirectMessage saved = messageRepository.save(msg);
        // Push SSE au destinataire — isFromMe calculé de son point de vue
        sseRegistry.push(receiverId, NotificationEvent.newMessage(
            MessageRecord.from(saved, receiverId)
        ));
        return MessageRecord.from(saved, senderId);
    }

    /**
     * Retourne les messages d'une conversation par ordre chronologique inverse
     * et marque automatiquement les messages reçus comme lus.
     */
    @Transactional(transactionManager = "userTransactionManager")
    public List<MessageRecord> getConversation(UUID partnerId, int size) {
        UUID currentUserId = currentUserId();
        int clampedSize = Math.min(Math.max(size, 1), 100);
        List<DirectMessage> messages = messageRepository.findConversation(
            currentUserId, partnerId, PageRequest.of(0, clampedSize)
        );
        // Marquer les messages reçus non lus comme lus (dirty-check Hibernate)
        messages.stream()
            .filter(m -> m.getReceiverId().equals(currentUserId) && m.getReadAt() == null)
            .forEach(m -> m.setReadAt(Instant.now()));
        return messages.stream()
            .map(m -> MessageRecord.from(m, currentUserId))
            .toList();
    }

    @Transactional(readOnly = true, transactionManager = "userTransactionManager")
    public List<ConversationRecord> getConversations() {
        UUID currentUserId = currentUserId();
        List<DirectMessage> allMessages = messageRepository.findAllInvolving(currentUserId);

        // Group by partner — premier message de chaque groupe est le plus récent (tri DESC)
        Map<UUID, DirectMessage> lastByPartner = new LinkedHashMap<>();
        for (DirectMessage m : allMessages) {
            UUID partnerId = m.getSenderId().equals(currentUserId)
                ? m.getReceiverId()
                : m.getSenderId();
            lastByPartner.putIfAbsent(partnerId, m);
        }

        List<UUID> partnerIds = new ArrayList<>(lastByPartner.keySet());
        Map<UUID, UserAccount> partners = userAccountRepository.findAllById(partnerIds).stream()
            .collect(Collectors.toMap(UserAccount::getId, u -> u));

        return partnerIds.stream()
            .map(partnerId -> {
                DirectMessage last = lastByPartner.get(partnerId);
                UserAccount partner = partners.get(partnerId);
                if (partner == null) return null;
                boolean hasUnread = last.getReceiverId().equals(currentUserId)
                    && last.getReadAt() == null;
                return new ConversationRecord(
                    partnerId,
                    FollowService.resolveDisplayName(partner),
                    partner.getAvatarUrl(),
                    last.getContent(),
                    last.getSentAt(),
                    hasUnread
                );
            })
            .filter(Objects::nonNull)
            .toList();
    }

    private UUID currentUserId() {
        return UUID.fromString(sessionSpringService.getCurrentUserId());
    }
}

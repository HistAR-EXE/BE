package com.histar.be.conversation.repository;

import com.histar.be.conversation.entity.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByUserIdAndCharacterId(UUID userId, UUID characterId);
}

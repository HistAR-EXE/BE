package com.histar.be.message.repository;

import com.histar.be.message.entity.Message;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByCreatedAt(UUID conversationId);

    @Query(
            """
            SELECT COUNT(m) FROM Message m
            JOIN Conversation c ON c.id = m.conversationId
            WHERE c.userId = :userId
              AND m.role = 'user'
              AND m.createdAt >= :start
              AND m.createdAt < :end
            """)
    long countUserMessagesByUserIdAndCreatedAtBetween(
            @Param("userId") UUID userId, @Param("start") Instant start, @Param("end") Instant end);
}

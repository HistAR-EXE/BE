package com.histar.be.userquestprogress.repository;

import com.histar.be.userquestprogress.entity.UserQuestProgress;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserQuestProgressRepository extends JpaRepository<UserQuestProgress, UUID> {

    Optional<UserQuestProgress> findByUserIdAndQuestId(UUID userId, UUID questId);

    List<UserQuestProgress> findByUserId(UUID userId);

    List<UserQuestProgress> findByUserIdAndStatus(UUID userId, String status);

    long countByUserIdAndStatus(UUID userId, String status);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE UserQuestProgress p
            SET p.status = :completed, p.completedAt = :completedAt
            WHERE p.userId = :userId AND p.questId = :questId AND p.status = :inProgress
            """)
    int markCompletedIfInProgress(
            @Param("userId") UUID userId,
            @Param("questId") UUID questId,
            @Param("completed") String completed,
            @Param("inProgress") String inProgress,
            @Param("completedAt") Instant completedAt);
}

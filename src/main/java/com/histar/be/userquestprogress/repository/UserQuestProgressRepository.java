package com.histar.be.userquestprogress.repository;

import com.histar.be.userquestprogress.entity.UserQuestProgress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserQuestProgressRepository extends JpaRepository<UserQuestProgress, UUID> {

    Optional<UserQuestProgress> findByUserIdAndQuestId(UUID userId, UUID questId);

    List<UserQuestProgress> findByUserId(UUID userId);

    List<UserQuestProgress> findByUserIdAndStatus(UUID userId, String status);

    long countByUserIdAndStatus(UUID userId, String status);
}

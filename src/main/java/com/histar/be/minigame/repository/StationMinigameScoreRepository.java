package com.histar.be.minigame.repository;

import com.histar.be.minigame.entity.StationMinigameScore;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationMinigameScoreRepository extends JpaRepository<StationMinigameScore, UUID> {

    Optional<StationMinigameScore> findByUserIdAndMinigameId(UUID userId, UUID minigameId);

    List<StationMinigameScore> findByUserIdOrderByCompletedAtDesc(UUID userId);
}

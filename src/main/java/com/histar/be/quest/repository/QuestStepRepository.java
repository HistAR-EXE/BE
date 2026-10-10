package com.histar.be.quest.repository;

import com.histar.be.quest.entity.QuestStep;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestStepRepository extends JpaRepository<QuestStep, UUID> {

    List<QuestStep> findByQuestId(UUID questId);

    List<QuestStep> findByMinigameId(UUID minigameId);
}

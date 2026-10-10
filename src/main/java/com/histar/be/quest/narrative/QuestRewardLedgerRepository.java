package com.histar.be.quest.narrative;
import com.histar.be.quest.narrative.entity.QuestRewardLedgerEntry;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QuestRewardLedgerRepository extends JpaRepository<QuestRewardLedgerEntry, UUID> { boolean existsByUserIdAndQuestIdAndRewardTypeAndRewardKey(UUID userId, UUID questId, String rewardType, String rewardKey); List<QuestRewardLedgerEntry> findByUserIdAndQuestId(UUID userId, UUID questId); }

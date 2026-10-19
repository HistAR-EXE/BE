package com.histar.be.quest.repository;

import com.histar.be.quest.entity.QuestStep;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestStepRepository extends JpaRepository<QuestStep, UUID> {

    List<QuestStep> findByQuestId(UUID questId);

    List<QuestStep> findByQuestIdIn(Collection<UUID> questIds);

    List<QuestStep> findByMinigameId(UUID minigameId);
    List<QuestStep> findByUnlockKey(String unlockKey);

    /** Có bước "xem hiện vật" (action_type = artifact) dùng khóa này trong một nhiệm vụ Online tại di tích. */
    @Query("select count(s) > 0 from QuestStep s where s.unlockKey = :unlockKey and s.actionType = 'artifact'"
            + " and s.quest.locationId = :locationId and s.quest.track = 'ONLINE' and s.quest.deletedAt is null")
    boolean existsOnlineArtifactStep(@Param("locationId") UUID locationId, @Param("unlockKey") String unlockKey);
}

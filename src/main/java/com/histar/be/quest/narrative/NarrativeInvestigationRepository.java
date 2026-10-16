package com.histar.be.quest.narrative;

import com.histar.be.quest.narrative.entity.NarrativeInvestigation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface NarrativeInvestigationRepository extends JpaRepository<NarrativeInvestigation, UUID> {
    Optional<NarrativeInvestigation> findByQuestId(UUID questId);
    Optional<NarrativeInvestigation> findByQuestIdAndEnabledTrue(UUID questId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<NarrativeInvestigation> findForUpdateByQuestId(UUID questId);
    long countByEnabledTrue();
    long countByReviewStatus(String reviewStatus);
    long countByRolloutState(com.histar.be.quest.narrative.entity.NarrativeRolloutState rolloutState);
}

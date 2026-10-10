package com.histar.be.quest.narrative;

import com.histar.be.quest.narrative.entity.NarrativeInvestigation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NarrativeInvestigationRepository extends JpaRepository<NarrativeInvestigation, UUID> {
    Optional<NarrativeInvestigation> findByQuestId(UUID questId);
    Optional<NarrativeInvestigation> findByQuestIdAndEnabledTrue(UUID questId);
}

package com.histar.be.quest.narrative;

import com.histar.be.quest.narrative.entity.NarrativeInvestigationVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NarrativeInvestigationVersionRepository extends JpaRepository<NarrativeInvestigationVersion, UUID> {
    Optional<NarrativeInvestigationVersion> findByInvestigationIdAndVersion(UUID investigationId, int version);
    Optional<NarrativeInvestigationVersion> findByInvestigationIdAndOpenDraftTrue(UUID investigationId);
    List<NarrativeInvestigationVersion> findByInvestigationIdOrderByVersionDesc(UUID investigationId);
    long countByOpenDraftTrue();
    long countByOpenDraftTrueAndReviewStatus(String reviewStatus);
}

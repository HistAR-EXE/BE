package com.histar.be.quest.narrative;

import com.histar.be.quest.narrative.entity.NarrativeInvestigationReview;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NarrativeInvestigationReviewRepository extends JpaRepository<NarrativeInvestigationReview, UUID> {
    List<NarrativeInvestigationReview> findByInvestigationIdOrderByReviewedAtDesc(UUID investigationId);
}

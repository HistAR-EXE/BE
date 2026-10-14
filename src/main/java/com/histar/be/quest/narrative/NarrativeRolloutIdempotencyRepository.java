package com.histar.be.quest.narrative;

import com.histar.be.quest.narrative.entity.NarrativeRolloutIdempotency;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NarrativeRolloutIdempotencyRepository extends JpaRepository<NarrativeRolloutIdempotency, UUID> {
    Optional<NarrativeRolloutIdempotency> findByInvestigationIdAndIdempotencyKey(UUID investigationId, String idempotencyKey);
}

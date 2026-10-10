package com.histar.be.quest.narrative;

import com.histar.be.quest.narrative.entity.NarrativeReviewDecision;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record NarrativeReviewResponse(UUID investigationId, NarrativeReviewDecision decision, Set<String> approvedLocales, String reviewedBy, Instant reviewedAt, String note) {}

package com.histar.be.organization.dto;
import java.util.UUID;
/** Aggregate-only classroom view: no student chat, memory, or per-student transcript. */
public record ClassQuestDashboardResponse(UUID organizationId, UUID campaignId, UUID questId, long learners, long completedLearners, double completionRatePct, long learnersWithEvidence, double evidenceCoveragePct, long verifiedOutcomes, String commonBlocker) {}

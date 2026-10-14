package com.histar.be.quest.narrative;

import com.histar.be.quest.narrative.entity.NarrativeRolloutState;
import java.util.UUID;

public record NarrativePilotListResponse(
        UUID questId,
        String questTitle,
        UUID investigationId,
        UUID locationId,
        String locationName,
        String reviewStatus,
        NarrativeRolloutState rolloutState,
        Integer publishedVersion,
        Integer draftVersion,
        String draftReviewStatus,
        long rowVersion,
        boolean globalFlagEnabled) {}

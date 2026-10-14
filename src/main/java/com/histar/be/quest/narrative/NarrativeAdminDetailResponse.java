package com.histar.be.quest.narrative;

import com.histar.be.quest.narrative.entity.NarrativeRolloutState;
import java.util.List;
import java.util.UUID;

public record NarrativeAdminDetailResponse(UUID questId, String questTitle, UUID investigationId,
        UUID locationId, String locationName, String reviewStatus, NarrativeRolloutState rolloutState,
        long rowVersion, Integer publishedVersion, Integer draftVersion, String draftReviewStatus,
        boolean globalFlagEnabled, NarrativeVersionResponse published, NarrativeVersionResponse draft,
        List<NarrativeVersionResponse> versions) {}

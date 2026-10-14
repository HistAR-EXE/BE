package com.histar.be.quest.narrative;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import com.histar.be.quest.narrative.model.NarrativeQuestGraph;

public record NarrativeVersionResponse(UUID id, int version, long revision, long rowVersion,
        boolean openDraft, String lifecycle, NarrativeQuestGraph graph,
        String reviewStatus, Set<String> approvedLocales, String reviewedBy, Instant reviewedAt,
        Instant createdAt, Instant updatedAt, boolean runtimeRewardFixed, Integer runtimeRewardXp) {}

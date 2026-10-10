package com.histar.be.quest.narrative;

import com.histar.be.quest.narrative.entity.NarrativeReviewDecision;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record NarrativeReviewRequest(@NotNull NarrativeReviewDecision decision, @NotNull Set<String> approvedLocales, String note) {}

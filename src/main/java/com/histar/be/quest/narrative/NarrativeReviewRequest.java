package com.histar.be.quest.narrative;

import com.histar.be.quest.narrative.entity.NarrativeReviewDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record NarrativeReviewRequest(
        @NotNull NarrativeReviewDecision decision,
        @NotNull Set<String> approvedLocales,
        @Size(min = 10, max = 1000) String note,
        int version,
        long revision) {
    public NarrativeReviewRequest(NarrativeReviewDecision decision, Set<String> approvedLocales, String note) {
        this(decision, approvedLocales, note, 0, 0);
    }
}

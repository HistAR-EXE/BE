package com.histar.be.quest.narrative;
import jakarta.validation.constraints.*;
public record NarrativeActionRequest(@NotNull NarrativeActionType action,
                                     @NotBlank String nodeCode,
                                     String observationCode,
                                     @NotNull Integer investigationVersion) {
    public NarrativeActionRequest(NarrativeActionType action, String nodeCode, Integer investigationVersion) {
        this(action, nodeCode, null, investigationVersion);
    }
}

package com.histar.be.quest.narrative;
import jakarta.validation.constraints.*;
public record NarrativeActionRequest(@NotNull NarrativeActionType action, @NotBlank String nodeCode, @NotNull Integer investigationVersion) {}

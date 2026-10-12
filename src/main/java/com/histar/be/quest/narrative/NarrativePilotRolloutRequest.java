package com.histar.be.quest.narrative;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NarrativePilotRolloutRequest(
        @NotBlank @Size(min = 10, max = 500) String reason) {}

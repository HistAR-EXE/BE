package com.histar.be.analytics.dto;

import java.util.UUID;

public record QuestCompletionMetadata(
        UUID questId,
        String trigger,
        UUID locationId,
        int discoveryStepsDone,
        int discoveryStepsTotal,
        boolean hasCheckin) {}

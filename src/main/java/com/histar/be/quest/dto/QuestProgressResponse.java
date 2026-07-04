package com.histar.be.quest.dto;

import java.time.Instant;
import java.util.UUID;

public record QuestProgressResponse(
        UUID questId,
        UUID locationId,
        String title,
        String description,
        String story,
        Integer pointsReward,
        String status,
        Integer currentStep,
        Integer stepsTotal,
        boolean discoveryStepsComplete,
        boolean hasCheckinAtLocation,
        String completionTrigger,
        Boolean requireOnsiteCheckin,
        Instant startedAt,
        Instant completedAt) {}

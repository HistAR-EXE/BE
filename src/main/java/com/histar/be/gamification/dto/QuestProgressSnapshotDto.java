package com.histar.be.gamification.dto;

import java.util.UUID;

public record QuestProgressSnapshotDto(
        UUID questId,
        String title,
        UUID locationId,
        String locationName,
        boolean stepCompleted,
        boolean questCompleted,
        int currentStep,
        int stepsTotal,
        int pointsAwarded) {}

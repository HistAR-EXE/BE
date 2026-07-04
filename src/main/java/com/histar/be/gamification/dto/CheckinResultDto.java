package com.histar.be.gamification.dto;

import java.util.List;
import java.util.UUID;

public record CheckinResultDto(
        boolean success,
        double distanceMeters,
        List<UUID> questsCompleted,
        List<BadgeEarnedDto> badgesEarned,
        boolean secretUnlocked,
        int bonusXpAwarded,
        int xpEarned,
        List<UnlockedArtifactDto> newArtifacts,
        QuestProgressSnapshotDto questProgress,
        List<com.histar.be.location.dto.LocationResponse> newlyUnlockedLocations) {}
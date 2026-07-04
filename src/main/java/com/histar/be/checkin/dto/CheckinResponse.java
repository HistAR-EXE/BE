package com.histar.be.checkin.dto;

import com.histar.be.gamification.dto.BadgeEarnedDto;
import com.histar.be.gamification.dto.CheckinResultDto;
import com.histar.be.gamification.dto.QuestProgressSnapshotDto;
import com.histar.be.gamification.dto.UnlockedArtifactDto;
import com.histar.be.location.dto.LocationResponse;
import java.util.List;
import java.util.UUID;

public record CheckinResponse(
        boolean success,
        double distanceMeters,
        List<UUID> questsCompleted,
        List<BadgeEarnedDto> badgesEarned,
        boolean secretUnlocked,
        int bonusXpAwarded,
        int xpEarned,
        List<UnlockedArtifactDto> newArtifacts,
        QuestProgressSnapshotDto questProgress,
        List<LocationResponse> newlyUnlockedLocations) {

    public static CheckinResponse from(CheckinResultDto result) {
        return new CheckinResponse(
                result.success(),
                result.distanceMeters(),
                result.questsCompleted(),
                result.badgesEarned(),
                result.secretUnlocked(),
                result.bonusXpAwarded(),
                result.xpEarned(),
                result.newArtifacts(),
                result.questProgress(),
                result.newlyUnlockedLocations());
    }
}

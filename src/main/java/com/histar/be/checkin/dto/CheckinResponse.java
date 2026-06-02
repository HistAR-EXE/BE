package com.histar.be.checkin.dto;

import com.histar.be.gamification.dto.BadgeEarnedDto;
import com.histar.be.gamification.dto.CheckinResultDto;
import java.util.List;
import java.util.UUID;

public record CheckinResponse(
        boolean success,
        double distanceMeters,
        List<UUID> questsCompleted,
        List<BadgeEarnedDto> badgesEarned,
        boolean secretUnlocked) {

    public static CheckinResponse from(CheckinResultDto result) {
        return new CheckinResponse(
                result.success(),
                result.distanceMeters(),
                result.questsCompleted(),
                result.badgesEarned(),
                result.secretUnlocked());
    }
}

package com.histar.be.profile.dto;

import com.histar.be.common.gamification.LevelCalculator;
import com.histar.be.config.GamificationProperties;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserTier;
import java.util.UUID;

public record ProfileMeResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        String role,
        String tier,
        Integer level,
        String levelName,
        Integer totalPoints,
        Integer pointsToNextLevel,
        Integer levelProgressPercent,
        String city) {

    public static ProfileMeResponse from(Profile profile, GamificationProperties properties) {
        int points = profile.getTotalPoints() == null ? 0 : profile.getTotalPoints();
        var levelInfo = LevelCalculator.info(
                points, properties.parseLevelThresholds(), properties.parseLevelNames());
        return new ProfileMeResponse(
                profile.getId(),
                profile.getEmail(),
                profile.getDisplayName(),
                profile.getAvatarUrl(),
                profile.getRole(),
                UserTier.fromStored(profile.getTier()).name(),
                levelInfo.level(),
                levelInfo.levelName(),
                points,
                levelInfo.pointsToNextLevel(),
                levelInfo.levelProgressPercent(),
                profile.getCity());
    }
}

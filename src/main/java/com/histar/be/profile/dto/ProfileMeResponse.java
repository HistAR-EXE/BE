package com.histar.be.profile.dto;

import com.histar.be.common.gamification.LevelCalculator;
import com.histar.be.config.GamificationProperties;
import com.histar.be.organization.entity.OrgSubscription;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import java.util.UUID;

public record ProfileMeResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        String role,
        String tier,
        UUID orgId,
        String orgName,
        String orgSubscription,
        String orgRole,
        Integer level,
        String levelName,
        Integer totalPoints,
        Integer pointsToNextLevel,
        Integer levelProgressPercent,
        String city) {

    public static ProfileMeResponse from(
            Profile profile,
            GamificationProperties properties,
            UUID orgId,
            String orgName,
            String orgSubscription,
            String orgRole) {
        int points = profile.getTotalPoints() == null ? 0 : profile.getTotalPoints();
        var levelInfo = LevelCalculator.info(
                points, properties.parseLevelThresholds(), properties.parseLevelNames());
        UUID resolvedOrgId = profile.getOrgId() != null ? profile.getOrgId() : orgId;
        String resolvedSub = profile.getOrgSubscription() != null
                ? OrgSubscription.fromStored(profile.getOrgSubscription()).name()
                : (orgSubscription != null ? orgSubscription : OrgSubscription.NONE.name());
        return new ProfileMeResponse(
                profile.getId(),
                profile.getEmail(),
                profile.getDisplayName(),
                profile.getAvatarUrl(),
                UserRole.fromStored(profile.getRole()).name(),
                UserTier.fromStored(profile.getTier()).name(),
                resolvedOrgId,
                orgName,
                resolvedSub,
                orgRole,
                levelInfo.level(),
                levelInfo.levelName(),
                points,
                levelInfo.pointsToNextLevel(),
                levelInfo.levelProgressPercent(),
                profile.getCity());
    }
}

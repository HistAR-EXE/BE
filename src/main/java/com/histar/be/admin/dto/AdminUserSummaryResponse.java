package com.histar.be.admin.dto;

import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import java.time.Instant;
import java.util.UUID;

public record AdminUserSummaryResponse(
        UUID id,
        String email,
        String displayName,
        String role,
        String tier,
        Boolean emailVerified,
        Integer level,
        Integer totalPoints,
        Instant createdAt) {

    public static AdminUserSummaryResponse from(Profile profile) {
        return new AdminUserSummaryResponse(
                profile.getId(),
                profile.getEmail(),
                profile.getDisplayName(),
                UserRole.fromStored(profile.getRole()).name(),
                profile.getTier(),
                profile.getEmailVerified(),
                profile.getLevel(),
                profile.getTotalPoints(),
                profile.getCreatedAt());
    }
}

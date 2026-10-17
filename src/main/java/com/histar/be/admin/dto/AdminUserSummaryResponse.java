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
        String accountStatus,
        Integer level,
        Integer totalPoints,
        Instant createdAt) {

    public static AdminUserSummaryResponse from(Profile profile) {
        String status = profile.getAccountStatus();
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
        return new AdminUserSummaryResponse(
                profile.getId(),
                profile.getEmail(),
                profile.getDisplayName(),
                UserRole.fromStored(profile.getRole()).name(),
                profile.getTier(),
                profile.getEmailVerified(),
                status.trim().toUpperCase(),
                profile.getLevel(),
                profile.getTotalPoints(),
                profile.getCreatedAt());
    }
}

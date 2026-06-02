package com.histar.be.profile.dto;

import com.histar.be.profile.entity.Profile;
import java.util.UUID;

public record ProfileMeResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        Integer level,
        Integer totalPoints,
        String city) {

    public static ProfileMeResponse from(Profile profile) {
        return new ProfileMeResponse(
                profile.getId(),
                profile.getEmail(),
                profile.getDisplayName(),
                profile.getAvatarUrl(),
                profile.getLevel(),
                profile.getTotalPoints(),
                profile.getCity());
    }
}

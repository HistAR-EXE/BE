package com.histar.be.badge.dto;

import com.histar.be.badge.entity.Badge;
import java.util.UUID;

public record BadgeResponse(
        UUID id, String name, String description, String iconUrl, String conditionType, Integer conditionValue) {

    public static BadgeResponse from(Badge badge) {
        return new BadgeResponse(
                badge.getId(),
                badge.getName(),
                badge.getDescription(),
                badge.getIconUrl(),
                badge.getConditionType(),
                badge.getConditionValue());
    }
}

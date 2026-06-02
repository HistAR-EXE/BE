package com.histar.be.badge.dto;

import java.time.Instant;
import java.util.UUID;

public record UserBadgeResponse(
        UUID id,
        String name,
        String description,
        String iconUrl,
        boolean earned,
        Instant earnedAt) {}

package com.histar.be.squad.dto;

import java.time.Instant;
import java.util.UUID;

public record SquadMemberStateResponse(
        UUID userId,
        String displayName,
        String avatarUrl,
        Instant joinedAt,
        String stationCode,
        Integer progressPercent,
        String progressLabel) {}

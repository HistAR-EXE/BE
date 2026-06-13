package com.histar.be.analytics.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionReplayResponse(
        UUID sessionId,
        UUID userId,
        UUID locationId,
        String mode,
        Instant startedAt,
        Instant endedAt,
        List<SessionReplayStep> steps) {}

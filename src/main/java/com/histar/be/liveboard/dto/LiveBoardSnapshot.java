package com.histar.be.liveboard.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LiveBoardSnapshot(
        UUID organizationId,
        Instant generatedAt,
        Instant windowStart,
        int memberCount,
        int activeMembers,
        Instant lastRallyAt,
        List<LiveBoardRow> rows) {}

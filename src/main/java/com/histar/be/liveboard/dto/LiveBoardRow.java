package com.histar.be.liveboard.dto;

import java.time.Instant;
import java.util.UUID;

/** One student row on the teacher live board (last 24h window). */
public record LiveBoardRow(
        UUID userId,
        String displayName,
        String email,
        int stationsCompleted,
        int score,
        String lastStationCode,
        Instant lastActivityAt) {}

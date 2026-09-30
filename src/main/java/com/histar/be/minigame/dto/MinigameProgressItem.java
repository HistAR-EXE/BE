package com.histar.be.minigame.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record MinigameProgressItem(
        UUID minigameId,
        String siteCode,
        String stationCode,
        String gameType,
        String title,
        int score,
        Instant completedAt) {}

package com.histar.be.minigame.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record MinigameSubmitResponse(UUID minigameId, int score, Instant completedAt, boolean newBest) {}

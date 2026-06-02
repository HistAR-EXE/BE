package com.histar.be.quest.dto;

import java.time.Instant;
import java.util.UUID;

public record QuestProgressResponse(
        UUID questId,
        UUID locationId,
        String title,
        String description,
        Integer pointsReward,
        String status,
        Instant startedAt,
        Instant completedAt) {}

package com.histar.be.quest.v2.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Thẻ nhiệm vụ cho danh sách.
 *
 * @param track ONLINE · ONSITE
 * @param status LOCKED · AVAILABLE · IN_PROGRESS · COMPLETED
 * @param lock null khi không bị khóa
 */
public record QuestV2Card(
        UUID id,
        UUID locationId,
        String track,
        String title,
        String description,
        String story,
        String coverImage,
        int sortOrder,
        String difficulty,
        Integer estimatedMinutes,
        int pointsReward,
        String status,
        QuestV2Lock lock,
        QuestV2Progress progress,
        UUID nextStepId,
        Instant startedAt,
        Instant completedAt) {}

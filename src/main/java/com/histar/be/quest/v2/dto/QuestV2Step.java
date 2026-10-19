package com.histar.be.quest.v2.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Một bước của nhiệm vụ kèm trạng thái của người dùng.
 *
 * @param state DONE · TODO · PREMIUM_LOCKED
 * @param preDiscovered bước đã xong trước khi nhận nhiệm vụ (hiển thị "Đã khám phá trước đó")
 */
public record QuestV2Step(
        UUID id,
        int order,
        String key,
        String actionType,
        String title,
        String objective,
        String description,
        String hint,
        String actionLabel,
        String previewImage,
        Integer portalEra,
        UUID minigameId,
        String chatPrompt,
        boolean required,
        boolean premium,
        String state,
        Instant doneAt,
        boolean preDiscovered) {}

package com.histar.be.quest.minigame;

import java.util.Map;

/**
 * {@code pending = true}: lượt chơi chưa kết thúc, chưa lưu. {@code hint} cho FE phản hồi ngay
 * (ví dụ nóng/lạnh khi soi nắp, hướng đào, thẻ đặt sai tầng); rỗng khi đã có kết quả.
 */
public record MinigameAttemptResponse(
        int stars,
        boolean passed,
        String learnFact,
        String learnUrl,
        boolean discoveryRecorded,
        int discoveryXp,
        int attempts,
        boolean pending,
        Map<String, Object> hint) {}

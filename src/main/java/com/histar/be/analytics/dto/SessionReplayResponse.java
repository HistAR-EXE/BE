package com.histar.be.analytics.dto;

import java.util.List;
import java.util.UUID;

public record SessionReplayResponse(
        UUID sessionId,
        List<ReplayStep> steps
) {
    public record ReplayStep(String unlockKey, String eventType) {}
}

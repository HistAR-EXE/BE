package com.histar.be.chat.memory;

import java.time.Instant;
import java.util.UUID;

public record JourneyMemoryResponse(
        UUID locationId,
        boolean enabled,
        String lastVisitedPoi,
        String journeySummary,
        String locale,
        String preferredMode,
        Instant expiresAt) {

    public static JourneyMemoryResponse from(JourneyMemory memory) {
        return new JourneyMemoryResponse(
                memory.getLocationId(),
                memory.isEnabled(),
                memory.getLastVisitedPoi(),
                memory.getJourneySummary(),
                memory.getLocale(),
                memory.getPreferredMode(),
                memory.getExpiresAt());
    }
}

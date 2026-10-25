package com.histar.be.group.dto;

import com.histar.be.group.entity.ParticipationMode;
import java.time.Instant;
import java.util.UUID;

public record GroupSummaryResponse(
        UUID id,
        String name,
        String code,
        Instant expiresAt,
        int memberCount,
        UUID orgId,
        UUID questId,
        UUID fieldTripId,
        ParticipationMode participationMode,
        Integer declaredSize) {

    public GroupSummaryResponse(
            UUID id, String name, String code, Instant expiresAt, int memberCount, UUID orgId, UUID questId) {
        this(id, name, code, expiresAt, memberCount, orgId, questId, null, null, null);
    }
}

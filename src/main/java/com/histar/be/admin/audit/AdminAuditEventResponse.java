package com.histar.be.admin.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AdminAuditEventResponse(
        UUID id,
        UUID actorId,
        String actorDisplayName,
        String actorEmail,
        String entityType,
        UUID entityId,
        String action,
        Long oldRowVersion,
        Long newRowVersion,
        String reason,
        Map<String, Object> metadata,
        Instant occurredAt,
        String eventHash,
        String prevEventHash) {}

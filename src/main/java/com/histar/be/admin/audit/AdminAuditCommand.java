package com.histar.be.admin.audit;

import java.util.Map;
import java.util.UUID;

public record AdminAuditCommand(
        UUID actorId,
        String entityType,
        UUID entityId,
        String action,
        Long oldRowVersion,
        Long newRowVersion,
        String reason,
        String idempotencyKey,
        Map<String, ?> metadata) {}

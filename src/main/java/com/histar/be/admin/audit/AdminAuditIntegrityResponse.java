package com.histar.be.admin.audit;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditIntegrityResponse(
        boolean valid,
        long checkedCount,
        String chainTip,
        String expectedTip,
        UUID brokenEventId,
        Instant from,
        Instant to) {}

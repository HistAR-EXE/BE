package com.histar.be.admin.audit;

import java.time.Instant;
import java.util.List;

public record AdminAuditExportEnvelope(
        Instant exportedAt, long totalMatched, int exportedCount, boolean truncated, List<AdminAuditEventResponse> items) {}

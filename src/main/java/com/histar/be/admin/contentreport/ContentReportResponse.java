package com.histar.be.admin.contentreport;

import java.time.Instant;
import java.util.UUID;

public record ContentReportResponse(
        UUID eventId,
        String stationCode,
        UUID sessionId,
        String payload,
        Instant occurredAt,
        ContentReportStatus status,
        String adminNote,
        UUID reviewedBy,
        Instant reviewedAt,
        Long rowVersion,
        Instant createdAt,
        Instant updatedAt) {}

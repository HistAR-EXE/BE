package com.histar.be.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrgArReportResponse(
        UUID organizationId,
        String organizationName,
        Instant from,
        Instant to,
        long memberCount,
        long checkins,
        long visitSessions,
        long onsiteSessions,
        long discoveries,
        long questCompletions,
        List<DailyPoint> dailyCheckins) {

    public record DailyPoint(String day, long count) {}
}

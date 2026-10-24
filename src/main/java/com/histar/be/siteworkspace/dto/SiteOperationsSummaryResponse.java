package com.histar.be.siteworkspace.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SiteOperationsSummaryResponse(
        String siteCode,
        Instant from,
        Instant to,
        long visitCount,
        List<StationCheckinCount> checkinsByStation,
        long questCompletions,
        Map<String, Long> contentInteractions) {

    public record StationCheckinCount(String stationCode, long count) {}
}

package com.histar.be.checkin.dto;

import java.time.Instant;
import java.util.UUID;

public record CheckinHistoryItem(
        UUID id,
        UUID locationId,
        String siteCode,
        String stationCode,
        String stationName,
        Instant checkedInAt,
        String presenceMethod) {}

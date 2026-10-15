package com.histar.be.analytics.dto;

import com.histar.be.analytics.AnalyticsRange;
import java.util.List;
import java.util.UUID;

public record ActivityHeatmapResponse(
        UUID locationId,
        AnalyticsRange range,
        int privacyThreshold,
        List<String> dayLabels,
        List<String> slotLabels,
        List<ActivityHeatmapCell> cells,
        String peakMorningLabel,
        String peakAfternoonLabel,
        int daysWithData) {}

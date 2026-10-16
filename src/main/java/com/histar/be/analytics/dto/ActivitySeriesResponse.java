package com.histar.be.analytics.dto;

import com.histar.be.analytics.AnalyticsBucket;
import com.histar.be.analytics.AnalyticsRange;
import java.util.List;
import java.util.UUID;

public record ActivitySeriesResponse(
        UUID locationId, AnalyticsRange range, AnalyticsBucket bucket,
        int privacyThreshold, List<ActivitySeriesPoint> series) {}

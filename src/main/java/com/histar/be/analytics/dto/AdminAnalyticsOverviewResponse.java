package com.histar.be.analytics.dto;

import java.util.List;
import java.util.UUID;

public record AdminAnalyticsOverviewResponse(
        UUID locationId,
        List<PoiUnlockRateItem> poiUnlockRates,
        List<QuestFunnelItem> questFunnel,
        OnlineOfflineConversion onlineToOnsite,
        List<JourneyDropOffItem> journeyDropOff,
        List<PoiHeatmapItem> poiHeatmap,
        SessionQualityMetrics sessionQuality,
        Long questCompletedCount,
        String journeyNote,
        int privacyThreshold,
        boolean questCompletedSuppressed,
        Long activeUsers,
        Long eventCount,
        boolean activitySuppressed,
        Long referralUsers,
        boolean referralSuppressed) {}

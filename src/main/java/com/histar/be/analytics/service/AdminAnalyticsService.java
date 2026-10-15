package com.histar.be.analytics.service;

import com.histar.be.analytics.dto.AdminAnalyticsOverviewResponse;
import java.util.UUID;
import com.histar.be.analytics.AnalyticsBucket;
import com.histar.be.analytics.AnalyticsRange;
import com.histar.be.analytics.dto.ActivityHeatmapResponse;
import com.histar.be.analytics.dto.ActivitySeriesResponse;

public interface AdminAnalyticsService {

    AdminAnalyticsOverviewResponse overview(UUID locationId);

    AdminAnalyticsOverviewResponse overview(UUID locationId, AnalyticsRange range);

    ActivitySeriesResponse activitySeries(UUID locationId, AnalyticsRange range, AnalyticsBucket bucket);

    ActivityHeatmapResponse activityHeatmap(UUID locationId, AnalyticsRange range);
}

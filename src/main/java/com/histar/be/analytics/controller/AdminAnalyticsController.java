package com.histar.be.analytics.controller;

import com.histar.be.analytics.dto.AdminAnalyticsOverviewResponse;
import com.histar.be.analytics.service.AdminAnalyticsService;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.events.dto.PilotKpiResponse;
import com.histar.be.events.service.EventsService;
import java.util.UUID;
import com.histar.be.analytics.AnalyticsBucket;
import com.histar.be.analytics.AnalyticsRange;
import com.histar.be.analytics.dto.ActivityHeatmapResponse;
import com.histar.be.analytics.dto.ActivitySeriesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;
    private final EventsService eventsService;

    @GetMapping("/pilot-kpi")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PilotKpiResponse> pilotKpi() {
        return ApiResponse.ok(eventsService.pilotKpiLast7Days());
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminAnalyticsOverviewResponse> overview(
            @RequestParam(required = false) UUID locationId,
            @RequestParam(defaultValue = "30D") String range) {
        return ApiResponse.ok(adminAnalyticsService.overview(locationId, AnalyticsRange.parse(range)));
    }

    @GetMapping("/activity-series")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ActivitySeriesResponse> activitySeries(
            @RequestParam(required = false) UUID locationId,
            @RequestParam(defaultValue = "30D") String range,
            @RequestParam(defaultValue = "DAY") AnalyticsBucket bucket) {
        return ApiResponse.ok(adminAnalyticsService.activitySeries(locationId, AnalyticsRange.parse(range), bucket));
    }

    @GetMapping("/activity-heatmap")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ActivityHeatmapResponse> activityHeatmap(
            @RequestParam(required = false) UUID locationId,
            @RequestParam(defaultValue = "30D") String range) {
        return ApiResponse.ok(adminAnalyticsService.activityHeatmap(locationId, AnalyticsRange.parse(range)));
    }
}

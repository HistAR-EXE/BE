package com.histar.be.analytics.controller;

import com.histar.be.analytics.AnalyticsBucket;
import com.histar.be.analytics.AnalyticsRange;
import com.histar.be.analytics.dto.ActivityHeatmapResponse;
import com.histar.be.analytics.dto.ActivitySeriesResponse;
import com.histar.be.analytics.dto.AdminAnalyticsOverviewResponse;
import com.histar.be.analytics.dto.SessionReplayResponse;
import com.histar.be.analytics.service.AdminAnalyticsService;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.discovery.entity.UserDiscovery;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.events.dto.PilotKpiResponse;
import com.histar.be.events.service.EventsService;
import com.histar.be.visit.entity.VisitSession;
import com.histar.be.visit.repository.VisitSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;
    private final EventsService eventsService;
    private final VisitSessionRepository visitSessionRepository;
    private final UserDiscoveryRepository userDiscoveryRepository;

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

    @GetMapping("/sessions/{sessionId}/replay")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SessionReplayResponse> sessionReplay(@PathVariable UUID sessionId) {
        VisitSession session = visitSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        Instant from = session.getStartedAt();
        Instant to = session.getEndedAt() != null ? session.getEndedAt() : Instant.now();
        List<UserDiscovery> discoveries = userDiscoveryRepository
                .findByUserIdAndLocationIdAndDiscoveredAtBetween(
                        session.getUserId(), session.getLocationId(), from, to);
        List<SessionReplayResponse.ReplayStep> steps = discoveries.stream()
                .map(d -> new SessionReplayResponse.ReplayStep(d.getDiscoveryKey(), "discovery"))
                .toList();
        return ApiResponse.ok(new SessionReplayResponse(sessionId, steps));
    }
}

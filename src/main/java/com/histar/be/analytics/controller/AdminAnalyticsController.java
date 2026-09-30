package com.histar.be.analytics.controller;

import com.histar.be.analytics.dto.AdminAnalyticsOverviewResponse;
import com.histar.be.analytics.dto.SessionReplayResponse;
import com.histar.be.analytics.service.AdminAnalyticsService;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.events.dto.PilotKpiResponse;
import com.histar.be.events.service.EventsService;
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

    @GetMapping("/pilot-kpi")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PilotKpiResponse> pilotKpi() {
        return ApiResponse.ok(eventsService.pilotKpiLast7Days());
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminAnalyticsOverviewResponse> overview(@RequestParam UUID locationId) {
        return ApiResponse.ok(adminAnalyticsService.overview(locationId));
    }

    @GetMapping("/sessions/{sessionId}/replay")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SessionReplayResponse> sessionReplay(@PathVariable UUID sessionId) {
        return ApiResponse.ok(adminAnalyticsService.sessionReplay(sessionId));
    }
}

package com.histar.be.admin.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.events.entity.PilotEvent;
import com.histar.be.events.repository.PilotEventRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/content-reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentReportController {

    private final PilotEventRepository pilotEventRepository;

    public record ContentReportItem(
            UUID id,
            String stationCode,
            UUID sessionId,
            String payload,
            Instant occurredAt,
            String statusHint) {}

    @GetMapping
    public ApiResponse<List<ContentReportItem>> list(
            @RequestParam(defaultValue = "30") int days, @RequestParam(defaultValue = "50") int limit) {
        Instant since = Instant.now().minus(Math.max(1, days), ChronoUnit.DAYS);
        int size = Math.min(200, Math.max(1, limit));
        List<ContentReportItem> items = pilotEventRepository
                .findByEventTypeAndOccurredAtGreaterThanEqualOrderByOccurredAtDesc(
                        "content_report", since, PageRequest.of(0, size))
                .stream()
                .map(this::toItem)
                .toList();
        return ApiResponse.ok(items);
    }

    private ContentReportItem toItem(PilotEvent e) {
        String payload = e.getPayloadJson() == null ? "" : e.getPayloadJson();
        String status = payload.toLowerCase(Locale.ROOT).contains("reviewed") ? "REVIEWED" : "NEW";
        return new ContentReportItem(
                e.getId(), e.getStationCode(), e.getSessionId(), payload, e.getOccurredAt(), status);
    }
}

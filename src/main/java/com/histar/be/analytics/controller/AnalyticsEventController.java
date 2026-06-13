package com.histar.be.analytics.controller;

import com.histar.be.analytics.dto.RecordAnalyticsEventRequest;
import com.histar.be.analytics.repository.AnalyticsEventRepository;
import com.histar.be.analytics.entity.AnalyticsEvent;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/analytics")
@RequiredArgsConstructor
public class AnalyticsEventController {

    private final AnalyticsEventRepository analyticsEventRepository;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping("/events")
    public ApiResponse<Void> record(@RequestBody @Valid RecordAnalyticsEventRequest request) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        analyticsEventRepository.save(AnalyticsEvent.builder()
                .userId(userId)
                .locationId(request.locationId())
                .visitSessionId(request.visitSessionId())
                .eventType(request.eventType())
                .eventKey(request.eventKey())
                .source(request.source())
                .contentType(request.contentType())
                .createdAt(Instant.now())
                .build());
        return ApiResponse.ok(null);
    }
}

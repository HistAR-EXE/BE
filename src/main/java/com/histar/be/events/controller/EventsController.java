package com.histar.be.events.controller;

import com.histar.be.common.exception.RateLimitedException;
import com.histar.be.common.ratelimit.EventsBatchRateLimiter;
import com.histar.be.common.ratelimit.RateLimitDecision;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.events.dto.EventBatchRequest;
import com.histar.be.events.dto.EventBatchResponse;
import com.histar.be.events.service.EventsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventsController {

    private final EventsService eventsService;
    private final CurrentUserAccessor currentUserAccessor;
    private final EventsBatchRateLimiter eventsBatchRateLimiter;

    @PostMapping("/batch")
    public ApiResponse<EventBatchResponse> batch(
            @RequestBody @Valid EventBatchRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = resolveClientIp(httpRequest);
        RateLimitDecision decision = eventsBatchRateLimiter.tryAcquire(clientIp);
        if (!decision.allowed()) {
            throw new RateLimitedException(
                    "Quá nhiều yêu cầu. Vui lòng thử lại sau " + decision.retryAfterSeconds() + " giây.",
                    decision.retryAfterSeconds());
        }
        UUID userId = currentUserAccessor.getUserId().orElse(null);
        return ApiResponse.ok(eventsService.ingestBatch(request, userId));
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}

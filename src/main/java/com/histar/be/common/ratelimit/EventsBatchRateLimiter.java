package com.histar.be.common.ratelimit;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventsBatchRateLimiter {
    private final RateLimitStore store;

    @Value("${histar.rate-limit.events-batch-per-minute:30}")
    private int limitPerMinute;

    public RateLimitDecision tryAcquire(String clientIp) {
        String key = clientIp != null ? clientIp : "unknown";
        return store.tryAcquire("events-batch:" + key, limitPerMinute, Duration.ofMinutes(1));
    }
}

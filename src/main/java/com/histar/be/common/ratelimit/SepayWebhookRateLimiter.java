package com.histar.be.common.ratelimit;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SepayWebhookRateLimiter {
    private final RateLimitStore store;

    @Value("${histar.rate-limit.sepay-webhook-per-minute:20}")
    private int limitPerMinute;

    public RateLimitDecision tryAcquire(String clientIp) {
        String key = clientIp != null ? clientIp : "unknown";
        return store.tryAcquire("sepay-webhook:" + key, limitPerMinute, Duration.ofMinutes(1));
    }
}

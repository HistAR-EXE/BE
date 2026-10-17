package com.histar.be.common.ratelimit;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatBurstRateLimiter {
    private final RateLimitStore store;

    @Value("${histar.rate-limit.chat-burst-per-minute:10}")
    private int limitPerMinute;

    public RateLimitDecision tryAcquire(UUID userId) {
        return store.tryAcquire("chat-burst:" + userId, limitPerMinute, Duration.ofMinutes(1));
    }
}

package com.histar.be.common.ratelimit;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "histar.rate-limit.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRateLimitStore implements RateLimitStore {
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public RateLimitDecision tryAcquire(String key, int limit, Duration window) {
        long now = System.currentTimeMillis();
        Window next = windows.compute(key, (ignored, previous) -> {
            if (previous == null || previous.expiresAt <= now) {
                return new Window(1, now + window.toMillis());
            }
            return new Window(previous.count + 1, previous.expiresAt);
        });
        boolean allowed = next.count <= limit;
        long retry = Math.max(1, (next.expiresAt - now + 999) / 1000);
        return new RateLimitDecision(allowed, Math.max(0, limit - next.count), retry);
    }

    private record Window(long count, long expiresAt) {}
}

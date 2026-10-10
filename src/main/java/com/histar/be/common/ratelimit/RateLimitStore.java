package com.histar.be.common.ratelimit;

import java.time.Duration;

public interface RateLimitStore {
    RateLimitDecision tryAcquire(String key, int limit, Duration window);
}

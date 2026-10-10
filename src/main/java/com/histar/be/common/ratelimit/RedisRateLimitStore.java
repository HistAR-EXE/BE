package com.histar.be.common.ratelimit;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "histar.rate-limit.redis.enabled", havingValue = "true")
public class RedisRateLimitStore implements RateLimitStore {
    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); "
                    + "if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]); end; return count",
            Long.class);
    private final StringRedisTemplate redis;

    public RedisRateLimitStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public RateLimitDecision tryAcquire(String key, int limit, Duration window) {
        Long count = redis.execute(SCRIPT, java.util.List.of("histar:rate:" + key),
                String.valueOf(window.toMillis()));
        long used = count == null ? limit + 1L : count;
        return new RateLimitDecision(used <= limit, Math.max(0, limit - used), Math.max(1, window.toSeconds()));
    }
}

package com.histar.be.common.ratelimit;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CooperativeLeaderboardRateLimiter {
    private final RateLimitStore store;
    @Value("${histar.rate-limit.cooperative-leaderboard-per-minute:60}")
    private int limitPerMinute;

    public RateLimitDecision tryAcquire(UUID actorId) {
        return store.tryAcquire("cooperative-leaderboard:" + actorId, limitPerMinute, Duration.ofMinutes(1));
    }
}

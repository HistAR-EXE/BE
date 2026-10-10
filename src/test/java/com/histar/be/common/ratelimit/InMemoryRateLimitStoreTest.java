package com.histar.be.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryRateLimitStoreTest {
    @Test
    void enforces_a_shared_window_per_actor_key() {
        InMemoryRateLimitStore store = new InMemoryRateLimitStore();
        String key = "leaderboard:" + UUID.randomUUID();

        assertThat(store.tryAcquire(key, 2, Duration.ofMinutes(1)).allowed()).isTrue();
        assertThat(store.tryAcquire(key, 2, Duration.ofMinutes(1)).allowed()).isTrue();
        RateLimitDecision third = store.tryAcquire(key, 2, Duration.ofMinutes(1));

        assertThat(third.allowed()).isFalse();
        assertThat(third.remaining()).isZero();
        assertThat(third.retryAfterSeconds()).isPositive();
    }
}

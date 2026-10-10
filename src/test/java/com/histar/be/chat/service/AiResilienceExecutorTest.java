package com.histar.be.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.histar.be.config.ChatProperties;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AiResilienceExecutorTest {

    @Test
    void retriesTransientFailureUntilTheRequestSucceeds() {
        ChatProperties properties = properties(3, 2, Duration.ofSeconds(1));
        AtomicInteger attempts = new AtomicInteger();

        try (AiResilienceExecutor executor = new AiResilienceExecutor(properties)) {
            String reply = executor.execute("rag-ai", () -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new TimeoutException("upstream timed out");
                }
                return "reply";
            });

            assertThat(reply).isEqualTo("reply");
            assertThat(attempts).hasValue(3);
        }
    }

    @Test
    void opensCircuitAfterConfiguredTransientFailures() {
        ChatProperties properties = properties(1, 2, Duration.ofSeconds(30));
        AtomicInteger attempts = new AtomicInteger();

        try (AiResilienceExecutor executor = new AiResilienceExecutor(properties)) {
            assertThatThrownBy(() -> executor.execute("rag-ai", () -> {
                        attempts.incrementAndGet();
                        throw new TimeoutException("upstream timed out");
                    }))
                    .isInstanceOf(AiServiceUnavailableException.class);
            assertThatThrownBy(() -> executor.execute("rag-ai", () -> {
                        attempts.incrementAndGet();
                        throw new TimeoutException("upstream timed out");
                    }))
                    .isInstanceOf(AiServiceUnavailableException.class);
            assertThatThrownBy(() -> executor.execute("rag-ai", () -> {
                        attempts.incrementAndGet();
                        return "must not run";
                    }))
                    .isInstanceOf(AiCircuitOpenException.class);
            assertThat(executor.isCircuitOpen("rag-ai")).isTrue();
            assertThat(attempts).hasValue(2);
        }
    }

    @Test
    void convertsSlowCallIntoTimeoutAndCountsItAsTransientFailure() {
        ChatProperties properties = properties(1, 1, Duration.ofSeconds(1));
        properties.getResilience().setTimeout(Duration.ofMillis(20));

        try (AiResilienceExecutor executor = new AiResilienceExecutor(properties)) {
            assertThatThrownBy(() -> executor.execute("gemini", () -> {
                        Thread.sleep(200);
                        return "late";
                    }))
                    .isInstanceOf(AiServiceUnavailableException.class)
                    .hasMessageContaining("timed out");
        }
    }

    private static ChatProperties properties(int maxAttempts, int failureThreshold, Duration openDuration) {
        ChatProperties properties = new ChatProperties();
        properties.getResilience().setMaxAttempts(maxAttempts);
        properties.getResilience().setFailureThreshold(failureThreshold);
        properties.getResilience().setOpenDuration(openDuration);
        properties.getResilience().setInitialBackoff(Duration.ZERO);
        properties.getResilience().setTimeout(Duration.ofSeconds(1));
        return properties;
    }
}

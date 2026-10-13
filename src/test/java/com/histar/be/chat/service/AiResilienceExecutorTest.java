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
            String reply = executor.execute("gemini", () -> {
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

    @Test
    void ragUsesOneAttemptAndIndependentBudget() {
        ChatProperties properties = properties(2, 1, Duration.ofSeconds(30));
        properties.getResilience().setTimeout(Duration.ofMillis(20));
        properties.getRagAi().setTimeoutSeconds(1);
        AtomicInteger attempts = new AtomicInteger();
        try (AiResilienceExecutor executor = new AiResilienceExecutor(properties)) {
            org.assertj.core.api.Assertions.assertThatCode(() -> assertThat(executor.execute("rag-ai", () -> {
                    Thread.sleep(100);
                    return "answer";
                })).isEqualTo("answer")).doesNotThrowAnyException();
            assertThatThrownBy(() -> executor.execute("rag-ai", () -> {
                attempts.incrementAndGet();
                throw new TimeoutException();
            })).isInstanceOf(AiServiceUnavailableException.class);
            assertThat(attempts).hasValue(1);
            assertThat(executor.isCircuitOpen("rag-ai")).isTrue();
            assertThat(executor.isCircuitOpen("gemini")).isFalse();
        }
    }

    @Test
    void ragDoesNotRetryTransientFailure() {
        AtomicInteger attempts = new AtomicInteger();
        try (AiResilienceExecutor executor = new AiResilienceExecutor(properties(2, 1, Duration.ofSeconds(30)))) {
            assertThatThrownBy(() -> executor.execute("rag-ai", () -> {
                attempts.incrementAndGet();
                throw new TimeoutException();
            })).isInstanceOf(AiServiceUnavailableException.class);
            assertThat(attempts).hasValue(1);
        }
    }

    @Test
    void propagatesRequestIdIntoWorkersAndDoesNotLeakIt() {
        try (AiResilienceExecutor executor = new AiResilienceExecutor(new ChatProperties())) {
            org.slf4j.MDC.put("requestId", "test-request");
            try {
                assertThat(executor.execute("rag-ai", () -> org.slf4j.MDC.get("requestId"))).isEqualTo("test-request");
            } finally { org.slf4j.MDC.clear(); }
            assertThat(executor.execute("rag-ai", () -> org.slf4j.MDC.get("requestId"))).isNull();
        }
    }

    @Test
    void ragDefaultBudgetIs100SecondsWithoutChangingOtherProviders() {
        var properties = new ChatProperties();
        assertThat(properties.getRagAi().getTimeoutSeconds()).isEqualTo(100);
        assertThat(properties.getResilience().getTimeout()).isEqualTo(Duration.ofSeconds(15));
        assertThat(properties.getResilience().getMaxAttempts()).isEqualTo(2);
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

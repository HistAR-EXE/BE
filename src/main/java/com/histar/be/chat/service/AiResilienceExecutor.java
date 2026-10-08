package com.histar.be.chat.service;

import com.histar.be.config.ChatProperties;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Applies a bounded retry and an independent circuit for each AI downstream. */
@Component
public class AiResilienceExecutor implements AutoCloseable {

    private final ChatProperties.Resilience properties;
    private final ExecutorService workers;
    private final Map<String, Circuit> circuits = new ConcurrentHashMap<>();

    public AiResilienceExecutor(ChatProperties chatProperties) {
        this.properties = chatProperties.getResilience();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "ai-resilience");
            thread.setDaemon(true);
            return thread;
        };
        this.workers = Executors.newFixedThreadPool(16, factory);
    }

    public <T> T execute(String dependency, Callable<T> operation) {
        Circuit circuit = circuits.computeIfAbsent(dependency, ignored -> new Circuit());
        circuit.acquire(dependency);
        int attempts = Math.max(1, properties.getMaxAttempts());
        Throwable lastFailure = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                T result = callWithinTimeout(operation);
                circuit.recordSuccess();
                return result;
            } catch (Throwable failure) {
                if (!isTransient(failure)) {
                    circuit.releaseProbe();
                    throw propagate(failure);
                }
                lastFailure = failure;
                if (attempt < attempts) {
                    backoff(attempt);
                }
            }
        }
        circuit.recordTransientFailure(properties.getFailureThreshold(), properties.getOpenDuration());
        throw new AiServiceUnavailableException(dependency, attempts, lastFailure);
    }

    public boolean isCircuitOpen(String dependency) {
        Circuit circuit = circuits.get(dependency);
        return circuit != null && circuit.isOpen();
    }

    private <T> T callWithinTimeout(Callable<T> operation) throws Throwable {
        Future<T> future = workers.submit(operation);
        try {
            long timeoutMillis = Math.max(1, properties.getTimeout().toMillis());
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw ex;
        } catch (ExecutionException ex) {
            throw ex.getCause();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        }
    }

    private boolean isTransient(Throwable failure) {
        if (failure instanceof TimeoutException || failure instanceof InterruptedException
                || failure instanceof WebClientRequestException || failure instanceof IOException) {
            return true;
        }
        if (failure instanceof WebClientResponseException response) {
            int status = response.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        return false;
    }

    private void backoff(int attempt) {
        long delayMillis = Math.max(0, properties.getInitialBackoff().toMillis()) * attempt;
        if (delayMillis == 0) {
            return;
        }
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiServiceUnavailableException("retry", attempt, ex);
        }
    }

    private RuntimeException propagate(Throwable failure) {
        if (failure instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new AiServiceUnavailableException("request", 1, failure);
    }

    @PreDestroy
    @Override
    public void close() {
        workers.shutdownNow();
    }

    private static final class Circuit {
        private int consecutiveFailures;
        private Instant openUntil = Instant.MIN;
        private boolean halfOpenProbe;

        synchronized void acquire(String dependency) {
            Instant now = Instant.now();
            if (now.isBefore(openUntil)) {
                throw new AiCircuitOpenException(dependency);
            }
            if (!openUntil.equals(Instant.MIN)) {
                if (halfOpenProbe) {
                    throw new AiCircuitOpenException(dependency);
                }
                halfOpenProbe = true;
            }
        }

        synchronized boolean isOpen() {
            return Instant.now().isBefore(openUntil);
        }

        synchronized void recordSuccess() {
            consecutiveFailures = 0;
            openUntil = Instant.MIN;
            halfOpenProbe = false;
        }

        synchronized void releaseProbe() {
            halfOpenProbe = false;
        }

        synchronized void recordTransientFailure(int threshold, Duration openDuration) {
            consecutiveFailures++;
            if (consecutiveFailures >= Math.max(1, threshold)) {
                openUntil = Instant.now().plus(openDuration);
            }
            halfOpenProbe = false;
        }
    }
}

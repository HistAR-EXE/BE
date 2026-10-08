package com.histar.be.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Low-cardinality business metrics consumed from the Actuator metrics endpoint. */
@Component
@RequiredArgsConstructor
public class ProductionMetrics {

    private final MeterRegistry registry;

    public void recordChat(Duration duration, String source, boolean successful) {
        String normalizedSource = source == null || source.isBlank() ? "unknown" : source;
        String outcome = successful ? "success" : "error";
        Timer.builder("histar.chat.latency")
                .description("End-to-end orchestrated chat latency")
                .tags("source", normalizedSource, "outcome", outcome)
                .register(registry)
                .record(duration);
        if ("fallback".equals(normalizedSource)) {
            Counter.builder("histar.chat.fallback.total")
                    .description("Chat answers served by the direct LLM fallback")
                    .register(registry)
                    .increment();
        }
    }

    public void recordRagCitation(boolean cited) {
        Counter.builder("histar.rag.citations.total")
                .description("RAG answers classified by whether they include a verified citation")
                .tag("outcome", cited ? "cited" : "missing")
                .register(registry)
                .increment();
    }

    public void recordQuestStarted() {
        recordQuestFunnel("started");
    }

    public void recordQuestCompleted() {
        recordQuestFunnel("completed");
    }

    public void recordMinigameAttempt(boolean passed, boolean unresolved) {
        String outcome = unresolved ? "unresolved" : passed ? "passed" : "failed";
        Counter.builder("histar.minigame.attempts.total")
                .description("Quest minigame attempt outcomes")
                .tag("outcome", outcome)
                .register(registry)
                .increment();
    }

    private void recordQuestFunnel(String event) {
        Counter.builder("histar.quest.funnel.total")
                .description("Quest lifecycle events; abandonment is derived from started minus completed")
                .tag("event", event)
                .register(registry)
                .increment();
    }
}

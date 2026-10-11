package com.histar.be.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Low-cardinality business metrics consumed from the Actuator metrics endpoint. */
@Component
@Lazy(false)
public class ProductionMetrics {

    private final MeterRegistry registry;

    public ProductionMetrics(MeterRegistry registry) {
        this.registry = registry;
        registerBaselineMeters();
    }

    /**
     * Registers every dashboard series at startup. Without this, a panel or alert for a rare
     * branch (for example voice fallback) is invisible until a user happens to trigger it.
     * Registration does not increment a counter, so it cannot be mistaken for production traffic.
     */
    void registerBaselineMeters() {
        // `recordChat` always adds these two labels. Register the baseline with
        // the same label *keys* so Prometheus never rejects the runtime series.
        Timer.builder("histar.chat.latency")
                .tags("source", "unknown", "outcome", "success")
                .register(registry);
        Timer.builder("histar.chat.first_token.latency").register(registry);
        Timer.builder("histar.chat.provider.latency").register(registry);
        registerCounter("histar.chat.fallback.total");
        registerCounter("histar.chat.safety_blocks.total");
        registerCounter("histar.chat.memory_opt_out.total");
        registerCounter("histar.chat.voice_fallback.total");
        registerCounter("histar.rag.citations.total", "outcome", "cited");
        registerCounter("histar.rag.citations.total", "outcome", "missing");
        registerCounter("histar.chat.stream.total", "outcome", "complete");
        registerCounter("histar.chat.stream.total", "outcome", "error");
        registerCounter("histar.chat.stream.total", "outcome", "cancelled");
        registerCounter("histar.chat.grounded_answers.total", "outcome", "grounded");
        registerCounter("histar.chat.grounded_answers.total", "outcome", "ungrounded");
        registerCounter("histar.teacher_ai_draft.evaluation.total", "outcome", "passed");
        registerCounter("histar.teacher_ai_draft.evaluation.total", "outcome", "failed");
        registerCounter("histar.quest.funnel.total", "event", "started");
        registerCounter("histar.quest.funnel.total", "event", "completed");
        registerCounter("histar.minigame.attempts.total", "outcome", "passed");
        registerCounter("histar.minigame.attempts.total", "outcome", "failed");
        registerCounter("histar.minigame.attempts.total", "outcome", "unresolved");
    }

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

    public void recordChatFirstToken(Duration duration) {
        Timer.builder("histar.chat.first_token.latency")
                .description("Server time until the first guided stream event is ready")
                .register(registry)
                .record(duration);
    }

    public void recordChatProviderLatency(Duration duration) {
        Timer.builder("histar.chat.provider.latency")
                .description("Time spent consuming the upstream AI provider response")
                .register(registry)
                .record(duration);
    }

    public void recordChatStream(boolean complete) {
        counter("histar.chat.stream.total", "outcome", complete ? "complete" : "error");
    }

    public void recordChatStreamCancelled() {
        counter("histar.chat.stream.total", "outcome", "cancelled");
    }

    public void recordSafetyBlocked() {
        Counter.builder("histar.chat.safety_blocks.total").register(registry).increment();
    }

    public void recordGroundedAnswer(boolean grounded) {
        counter("histar.chat.grounded_answers.total", "outcome", grounded ? "grounded" : "ungrounded");
    }

    public void recordMemoryOptOut() {
        Counter.builder("histar.chat.memory_opt_out.total").register(registry).increment();
    }

    public void recordVoiceFallback() {
        Counter.builder("histar.chat.voice_fallback.total").register(registry).increment();
    }

    public void recordTeacherDraftEvaluation(boolean passed) {
        counter("histar.teacher_ai_draft.evaluation.total", "outcome", passed ? "passed" : "failed");
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

    private void counter(String name, String tag, String value) {
        registerCounter(name, tag, value).increment();
    }

    private Counter registerCounter(String name) {
        return Counter.builder(name).register(registry);
    }

    private Counter registerCounter(String name, String tag, String value) {
        return Counter.builder(name).tag(tag, value).register(registry);
    }
}

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

    public void recordChatFirstToken(Duration duration) {
        Timer.builder("histar.chat.first_token.latency")
                .description("Server time until the first guided stream event is ready")
                .register(registry)
                .record(duration);
    }

    public void recordChatStream(boolean complete) {
        counter("histar.chat.stream.total", "outcome", complete ? "complete" : "error");
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
        Counter.builder(name).tag(tag, value).register(registry).increment();
    }
}

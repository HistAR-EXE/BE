package com.histar.be.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ProductionMetricsTest {

    @Test
    void recordsChatLatencyFallbackAndCitationOutcome() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ProductionMetrics metrics = new ProductionMetrics(registry);

        metrics.recordChat(Duration.ofMillis(125), "fallback", true);
        metrics.recordRagCitation(true);

        assertThat(registry.find("histar.chat.latency").tag("source", "fallback").timer().count()).isEqualTo(1);
        assertThat(registry.find("histar.chat.fallback.total").counter().count()).isEqualTo(1);
        assertThat(registry.find("histar.rag.citations.total").tag("outcome", "cited").counter().count())
                .isEqualTo(1);
    }

    @Test
    void recordsQuestFunnelAndMinigameAttemptOutcome() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ProductionMetrics metrics = new ProductionMetrics(registry);

        metrics.recordQuestStarted();
        metrics.recordQuestCompleted();
        metrics.recordMinigameAttempt(false, false);

        assertThat(registry.find("histar.quest.funnel.total").tag("event", "started").counter().count())
                .isEqualTo(1);
        assertThat(registry.find("histar.quest.funnel.total").tag("event", "completed").counter().count())
                .isEqualTo(1);
        assertThat(registry.find("histar.minigame.attempts.total").tag("outcome", "failed").counter().count())
                .isEqualTo(1);
    }

    @Test
    void exportsDashboardMetricNamesToPrometheus() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        ProductionMetrics metrics = new ProductionMetrics(registry);
        metrics.recordChat(Duration.ofMillis(125), "fallback", true);
        metrics.recordRagCitation(true);
        metrics.recordQuestStarted();
        metrics.recordMinigameAttempt(false, false);

        String scrape = registry.scrape();

        assertThat(scrape)
                .contains("histar_chat_latency_seconds")
                .contains("histar_chat_fallback_total")
                .contains("histar_rag_citations_total")
                .contains("histar_quest_funnel_total")
                .contains("histar_minigame_attempts_total");
    }

    @Test
    void recordsStageTwoGuidedChatAndTeacherSignals() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ProductionMetrics metrics = new ProductionMetrics(registry);

        metrics.recordChatFirstToken(Duration.ofMillis(80));
        metrics.recordChatStream(false);
        metrics.recordSafetyBlocked();
        metrics.recordGroundedAnswer(true);
        metrics.recordMemoryOptOut();
        metrics.recordVoiceFallback();
        metrics.recordTeacherDraftEvaluation(false);
        metrics.recordChatProviderLatency(Duration.ofMillis(42));
        metrics.recordChatStreamCancelled();

        assertThat(registry.find("histar.chat.first_token.latency").timer().count()).isEqualTo(1);
        assertThat(registry.find("histar.chat.stream.total").tag("outcome", "error").counter().count()).isEqualTo(1);
        assertThat(registry.find("histar.chat.safety_blocks.total").counter().count()).isEqualTo(1);
        assertThat(registry.find("histar.chat.grounded_answers.total").tag("outcome", "grounded").counter().count()).isEqualTo(1);
        assertThat(registry.find("histar.chat.memory_opt_out.total").counter().count()).isEqualTo(1);
        assertThat(registry.find("histar.chat.voice_fallback.total").counter().count()).isEqualTo(1);
        assertThat(registry.find("histar.teacher_ai_draft.evaluation.total").tag("outcome", "failed").counter().count()).isEqualTo(1);
        assertThat(registry.find("histar.chat.provider.latency").timer().count()).isEqualTo(1);
        assertThat(registry.find("histar.chat.stream.total").tag("outcome", "cancelled").counter().count()).isEqualTo(1);
    }
}

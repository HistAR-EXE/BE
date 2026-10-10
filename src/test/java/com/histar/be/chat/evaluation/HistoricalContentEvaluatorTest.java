package com.histar.be.chat.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.histar.be.chat.dto.ChatSource;
import com.histar.be.chat.guided.GuidedAnswerBlock;
import com.histar.be.chat.guided.GuidedAnswerType;
import com.histar.be.chat.guided.GuidedConfidence;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HistoricalContentEvaluatorTest {

    private final HistoricalContentEvaluator evaluator = new HistoricalContentEvaluator();

    @Test
    void rejects_verified_facts_without_citations() {
        HistoricalEvaluation result = evaluator.evaluate(
                List.of(new GuidedAnswerBlock(GuidedAnswerType.VERIFIED_FACT, GuidedConfidence.VERIFIED, "Khẳng định", List.of())),
                Set.of("vi", "en", "ko", "zh-CN"));

        assertThat(result.passed()).isFalse();
        assertThat(result.reasons()).contains("VERIFIED_FACT requires at least one citation.");
    }

    @Test
    void requires_all_approved_locales_for_publishable_content() {
        HistoricalEvaluation result = evaluator.evaluate(
                List.of(new GuidedAnswerBlock(GuidedAnswerType.VERIFIED_FACT, GuidedConfidence.VERIFIED, "Khẳng định",
                        List.of(new ChatSource("Nguồn", "Trích", null)))),
                Set.of("vi", "en"));

        assertThat(result.passed()).isFalse();
        assertThat(result.reasons()).anyMatch(reason -> reason.contains("ko"));
    }
}

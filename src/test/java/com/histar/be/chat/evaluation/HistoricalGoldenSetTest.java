package com.histar.be.chat.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.chat.guided.GuidedAnswerBlock;
import com.histar.be.chat.guided.GuidedChatSafetyGuard;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HistoricalGoldenSetTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HistoricalContentEvaluator evaluator = new HistoricalContentEvaluator();
    private final GuidedChatSafetyGuard safetyGuard = new GuidedChatSafetyGuard();

    @Test
    void every_golden_case_matches_the_deterministic_historical_gate() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/golden/historical-evaluation.json")) {
            List<JsonNode> cases = objectMapper.readValue(input, new TypeReference<>() {});
            for (JsonNode scenario : cases) {
                if (scenario.has("message")) {
                    assertThat(safetyGuard.evaluate(scenario.get("message").asText()).blocked())
                            .as(scenario.get("name").asText())
                            .isEqualTo(scenario.get("safetyBlocked").asBoolean());
                    continue;
                }
                List<GuidedAnswerBlock> blocks = objectMapper.readerForListOf(GuidedAnswerBlock.class)
                        .readValue(scenario.get("blocks"));
                Set<String> locales = objectMapper.convertValue(
                        scenario.get("approvedLocales"), new TypeReference<Set<String>>() {});
                assertThat(evaluator.evaluate(blocks, locales).passed())
                        .as(scenario.get("name").asText())
                        .isEqualTo(scenario.get("passed").asBoolean());
            }
        }
    }
}

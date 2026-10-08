package com.histar.be.chat.guided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.histar.be.chat.dto.ChatSource;
import com.histar.be.observability.ProductionMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuidedChatStreamServiceTest {

    @Mock
    private GuidedChatService guidedChatService;

    @Test
    void emits_the_stable_sse_event_order() {
        UUID userId = UUID.randomUUID();
        GuidedAnswer answer = new GuidedAnswer(
                List.of(new GuidedAnswerBlock(
                        GuidedAnswerType.VERIFIED_FACT,
                        GuidedConfidence.VERIFIED,
                        "Nội dung trả lời.",
                        List.of(new ChatSource("Nguồn", "Trích", null)))),
                "Câu hỏi tiếp theo?");
        when(guidedChatService.respond(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new GuidedChatResponse(UUID.randomUUID(), answer, List.of("Gợi ý"), false));

        List<GuidedChatStreamEvent> events = new GuidedChatStreamService(guidedChatService, new ProductionMetrics(new SimpleMeterRegistry())).events(
                userId, new GuidedChatRequest(UUID.randomUUID(), "Câu hỏi", null, null, null, ChatMode.DEEP_EXPLAIN));

        assertThat(events).extracting(GuidedChatStreamEvent::name)
                .containsExactly("meta", "delta", "sources", "suggestions", "complete");
    }

    @Test
    void emits_safety_response_without_requiring_a_conversation_id() {
        UUID userId = UUID.randomUUID();
        GuidedAnswer answer = new GuidedAnswer(
                List.of(new GuidedAnswerBlock(GuidedAnswerType.INTERPRETATION, GuidedConfidence.CAUTION, "An toàn.", List.of())),
                null);
        when(guidedChatService.respond(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new GuidedChatResponse(null, answer, List.of(), true));

        List<GuidedChatStreamEvent> events = new GuidedChatStreamService(guidedChatService, new ProductionMetrics(new SimpleMeterRegistry())).events(
                userId, new GuidedChatRequest(UUID.randomUUID(), "Prompt injection", null, null, null, ChatMode.LIGHT_HINT));

        assertThat(events).hasSize(5);
    }
}

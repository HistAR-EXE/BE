package com.histar.be.chat.guided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
    void persistenceFailureNeverEmitsCompleteOrRecordsStreamSuccess() {
        var answer = new GuidedAnswer(List.of(new GuidedAnswerBlock(GuidedAnswerType.INTERPRETATION,
                GuidedConfidence.CAUTION, "Answer", List.of())), null);
        var draft = GuidedChatDraft.blocked(new GuidedChatResponse(UUID.randomUUID(), answer, List.of(), false));
        when(guidedChatService.prepareStreaming(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(draft);
        org.mockito.Mockito.doThrow(new IllegalStateException("persist failed")).when(guidedChatService).complete(draft);
        var metrics = org.mockito.Mockito.mock(ProductionMetrics.class);
        var events = new java.util.ArrayList<GuidedChatStreamEvent>();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new GuidedChatStreamService(guidedChatService, metrics)
                .stream(UUID.randomUUID(), new GuidedChatRequest(UUID.randomUUID(), "question", null, null, null, null), events::add))
                .hasMessage("persist failed");
        assertThat(events).extracting(GuidedChatStreamEvent::name).doesNotContain("complete");
        verify(metrics, never()).recordChatStream(true);
    }

    @Test
    void incrementalStreamKeepsFinalSourcesFromTheSameAnswer() {
        var source = new ChatSource("Evidence", "excerpt", "https://example.com/evidence");
        var block = new GuidedAnswerBlock(GuidedAnswerType.VERIFIED_FACT, GuidedConfidence.VERIFIED, "Fact [1]", List.of(source));
        var draft = GuidedChatDraft.blocked(new GuidedChatResponse(UUID.randomUUID(), new GuidedAnswer(List.of(block), null), List.of(), false));
        when(guidedChatService.prepareStreaming(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    java.util.function.Consumer<GuidedAnswerBlock> publisher = invocation.getArgument(2);
                    publisher.accept(block);
                    return draft;
                });
        var events = new GuidedChatStreamService(guidedChatService, new ProductionMetrics(new SimpleMeterRegistry()))
                .events(UUID.randomUUID(), new GuidedChatRequest(UUID.randomUUID(), "question", null, null, null, null));
        assertThat(events.stream().filter(event -> event.name().equals("sources")).findFirst().orElseThrow().data())
                .isEqualTo(List.of(source));
        assertThat(events.stream().filter(event -> event.name().equals("delta"))).hasSize(1);
    }

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
        GuidedChatResponse response = new GuidedChatResponse(UUID.randomUUID(), answer, List.of("Gợi ý"), false);
        when(guidedChatService.prepareStreaming(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(GuidedChatDraft.blocked(response));

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
        GuidedChatResponse response = new GuidedChatResponse(null, answer, List.of(), true);
        when(guidedChatService.prepareStreaming(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(GuidedChatDraft.blocked(response));

        List<GuidedChatStreamEvent> events = new GuidedChatStreamService(guidedChatService, new ProductionMetrics(new SimpleMeterRegistry())).events(
                userId, new GuidedChatRequest(UUID.randomUUID(), "Prompt injection", null, null, null, ChatMode.LIGHT_HINT));

        assertThat(events).hasSize(5);
    }

    @Test
    void does_not_persist_a_draft_when_the_client_disconnects_before_complete() {
        UUID userId = UUID.randomUUID();
        GuidedAnswer answer = new GuidedAnswer(
                List.of(new GuidedAnswerBlock(GuidedAnswerType.INTERPRETATION, GuidedConfidence.CAUTION, "Nội dung.", List.of())),
                null);
        when(guidedChatService.prepareStreaming(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(GuidedChatDraft.blocked(new GuidedChatResponse(null, answer, List.of(), false)));
        java.util.concurrent.atomic.AtomicInteger events = new java.util.concurrent.atomic.AtomicInteger();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new GuidedChatStreamService(
                        guidedChatService, new ProductionMetrics(new SimpleMeterRegistry()))
                .stream(userId, new GuidedChatRequest(UUID.randomUUID(), "Câu hỏi", null, null, null, ChatMode.LIGHT_HINT),
                        event -> { if (events.incrementAndGet() > 1) throw new IllegalStateException("client disconnected"); }))
                .isInstanceOf(IllegalStateException.class);

        verify(guidedChatService, never()).complete(org.mockito.ArgumentMatchers.any());
    }
}

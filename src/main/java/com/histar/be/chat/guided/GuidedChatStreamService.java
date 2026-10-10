package com.histar.be.chat.guided;

import com.histar.be.chat.dto.ChatSource;
import com.histar.be.observability.ProductionMetrics;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

/** Emits the stable SSE contract and never sends sources before the answer has been validated. */
@Service
public class GuidedChatStreamService {

    private final GuidedChatService guidedChatService;
    private final ProductionMetrics productionMetrics;

    public GuidedChatStreamService(GuidedChatService guidedChatService, ProductionMetrics productionMetrics) {
        this.guidedChatService = guidedChatService;
        this.productionMetrics = productionMetrics;
    }

    public List<GuidedChatStreamEvent> events(UUID userId, GuidedChatRequest request) {
        List<GuidedChatStreamEvent> events = new ArrayList<>();
        stream(userId, request, events::add);
        return List.copyOf(events);
    }

    public void stream(UUID userId, GuidedChatRequest request, Consumer<GuidedChatStreamEvent> sink) {
        Instant started = Instant.now();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("conversationId", request.conversationId());
        meta.put("safetyBlocked", false);
        sink.accept(new GuidedChatStreamEvent("meta", meta));
        boolean[] streamed = {false};
        GuidedChatDraft draft = guidedChatService.prepareStreaming(userId, request, block -> {
            if (!streamed[0]) {
                productionMetrics.recordChatFirstToken(Duration.between(started, Instant.now()));
                streamed[0] = true;
            }
            sink.accept(new GuidedChatStreamEvent("delta", block));
        });
        GuidedChatResponse response = draft.response();
        List<ChatSource> sources = new ArrayList<>();
        if (!streamed[0]) for (GuidedAnswerBlock block : response.answer().blocks()) {
            if (!streamed[0]) {
                productionMetrics.recordChatFirstToken(Duration.between(started, Instant.now()));
                streamed[0] = true;
            }
            sink.accept(new GuidedChatStreamEvent("delta", block));
            sources.addAll(block.sources());
        }
        sink.accept(new GuidedChatStreamEvent("sources", List.copyOf(sources)));
        sink.accept(new GuidedChatStreamEvent("suggestions", response.suggestions()));
        Map<String, Object> completion = new LinkedHashMap<>();
        completion.put("conversationId", response.conversationId());
        completion.put("followUpQuestion", response.answer().followUpQuestion());
        completion.put("safetyBlocked", response.safetyBlocked());
        sink.accept(new GuidedChatStreamEvent("complete", completion));
        guidedChatService.complete(draft);
        productionMetrics.recordChatStream(true);
    }
}

package com.histar.be.chat.guided;

import com.histar.be.chat.dto.ChatSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Turns a completed safe response into the stable SSE event contract. */
@Service
public class GuidedChatStreamService {

    private final GuidedChatService guidedChatService;

    public GuidedChatStreamService(GuidedChatService guidedChatService) {
        this.guidedChatService = guidedChatService;
    }

    public List<GuidedChatStreamEvent> events(UUID userId, GuidedChatRequest request) {
        GuidedChatResponse response = guidedChatService.respond(userId, request);
        List<GuidedChatStreamEvent> events = new ArrayList<>();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("conversationId", response.conversationId());
        meta.put("safetyBlocked", response.safetyBlocked());
        events.add(new GuidedChatStreamEvent("meta", meta));
        List<ChatSource> sources = new ArrayList<>();
        for (GuidedAnswerBlock block : response.answer().blocks()) {
            events.add(new GuidedChatStreamEvent("delta", block));
            sources.addAll(block.sources());
        }
        events.add(new GuidedChatStreamEvent("sources", List.copyOf(sources)));
        events.add(new GuidedChatStreamEvent("suggestions", response.suggestions()));
        Map<String, Object> completion = new LinkedHashMap<>();
        completion.put("followUpQuestion", response.answer().followUpQuestion());
        completion.put("safetyBlocked", response.safetyBlocked());
        events.add(new GuidedChatStreamEvent("complete", completion));
        return List.copyOf(events);
    }
}

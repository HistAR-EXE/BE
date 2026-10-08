package com.histar.be.chat.guided;

import com.histar.be.chat.dto.ChatContextResponse;
import com.histar.be.chat.dto.ChatMessageRequest;
import com.histar.be.chat.dto.ChatResponse;
import com.histar.be.chat.service.ChatService;
import com.histar.be.chat.memory.JourneyMemoryService;
import com.histar.be.observability.ProductionMetrics;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Coordinates a safe, grounded visitor answer without changing the legacy chat API. */
@Service
@RequiredArgsConstructor
public class GuidedChatService {

    private final ChatService chatService;
    private final GuidedChatSafetyGuard safetyGuard;
    private final GuidedAnswerFormatter answerFormatter;
    private final JourneySuggestionService suggestionService;
    private final JourneyMemoryService journeyMemoryService;
    private final ProductionMetrics productionMetrics;

    public GuidedChatResponse respond(UUID userId, GuidedChatRequest request) {
        GuidedSafetyDecision safety = safetyGuard.evaluate(request.message());
        if (safety.blocked()) {
            productionMetrics.recordSafetyBlocked();
            GuidedAnswer answer = answerFormatter.format(safety.safeReply(), List.of(), request.resolvedMode());
            return new GuidedChatResponse(null, answer, List.of("Hãy hỏi về địa điểm hoặc tư liệu đang khám phá."), true);
        }

        ChatResponse response = chatService.sendOrchestrated(
                userId,
                new ChatMessageRequest(
                        request.characterId(),
                        request.message(),
                        request.conversationId(),
                        request.stationCode(),
                        request.siteCode()));
        ChatContextResponse context = chatService.getContext(userId, request.characterId(), response.conversationId());
        Map<String, Object> journeyContext = context.playerContext() == null ? Map.of() : context.playerContext();
        if (context.locationId() != null) {
            journeyMemoryService.observe(
                    userId,
                    context.locationId(),
                    asText(journeyContext.get("lastVisitedPoi")),
                    asText(journeyContext.get("questState")),
                    "vi",
                    request.resolvedMode().name());
        }
        GuidedAnswer answer = answerFormatter.format(response.reply(), response.sources(), request.resolvedMode());
        productionMetrics.recordGroundedAnswer(!response.sources().isEmpty());
        return new GuidedChatResponse(
                response.conversationId(), answer, suggestionService.suggest(journeyContext), false);
    }

    private static String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}

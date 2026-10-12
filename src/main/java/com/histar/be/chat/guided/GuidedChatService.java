package com.histar.be.chat.guided;

import com.histar.be.chat.dto.ChatContextResponse;
import com.histar.be.chat.dto.ChatMessageRequest;
import com.histar.be.chat.dto.ChatResponse;
import com.histar.be.chat.service.PreparedChatResponse;
import com.histar.be.chat.service.ChatService;
import com.histar.be.chat.memory.JourneyMemoryService;
import com.histar.be.config.PilotFeatureProperties;
import com.histar.be.observability.ProductionMetrics;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
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
    private final PilotFeatureProperties pilotFeatures;

    public GuidedChatResponse respond(UUID userId, GuidedChatRequest request) {
        GuidedChatDraft draft = prepare(userId, request);
        complete(draft);
        return draft.response();
    }

    GuidedChatDraft prepare(UUID userId, GuidedChatRequest request) {
        return prepare(userId, request, null);
    }

    GuidedChatDraft prepareStreaming(UUID userId, GuidedChatRequest request, Consumer<GuidedAnswerBlock> publisher) {
        return prepare(userId, request, publisher);
    }

    private GuidedChatDraft prepare(UUID userId, GuidedChatRequest request, Consumer<GuidedAnswerBlock> publisher) {
        GuidedSafetyDecision safety = safetyGuard.evaluate(request.message());
        if (safety.blocked()) {
            productionMetrics.recordSafetyBlocked();
            GuidedAnswer answer = answerFormatter.format(safety.safeReply(), List.of(), effectiveMode(request));
            return GuidedChatDraft.blocked(new GuidedChatResponse(
                    null, answer, List.of("Hãy hỏi về địa điểm hoặc tư liệu đang khám phá."), true));
        }

        ChatMessageRequest messageRequest = new ChatMessageRequest(
                        request.characterId(),
                        request.message(),
                        request.conversationId(),
                        request.stationCode(),
                        request.siteCode(),
                        request.narrativeQuestId(),
                        request.narrativeNodeCode(),
                        request.narrativeVersion());
        PreparedChatResponse prepared = publisher == null
                ? chatService.prepareOrchestrated(userId, messageRequest)
                : chatService.prepareOrchestratedStreaming(userId, messageRequest, chunk -> publisher.accept(
                        new GuidedAnswerBlock(GuidedAnswerType.VERIFIED_FACT, GuidedConfidence.VERIFIED,
                                chunk.content(), chunk.sources())));
        ChatContextResponse context = chatService.getContext(userId, request.characterId(), prepared.conversationId());
        Map<String, Object> journeyContext = context.playerContext() == null ? Map.of() : context.playerContext();
        GuidedAnswer answer = answerFormatter.format(prepared.reply(), prepared.sources(), effectiveMode(request));
        productionMetrics.recordGroundedAnswer(!prepared.sources().isEmpty());
        GuidedChatResponse response = new GuidedChatResponse(
                prepared.conversationId(), answer, suggestionService.suggest(journeyContext), false);
        return new GuidedChatDraft(userId, request, messageRequest, prepared, response, context.locationId(),
                asText(journeyContext.get("lastVisitedPoi")), asText(journeyContext.get("questState")));
    }

    void complete(GuidedChatDraft draft) {
        if (draft.prepared() == null) {
            return;
        }
        chatService.persistPrepared(draft.userId(), draft.messageRequest(), draft.prepared());
        if (pilotFeatures.isJourneyMemory() && draft.locationId() != null) {
            journeyMemoryService.observe(draft.userId(), draft.locationId(), draft.lastVisitedPoi(), draft.questState(),
                    "vi", effectiveMode(draft.request()).name());
        }
    }

    private static String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private ChatMode effectiveMode(GuidedChatRequest request) {
        return pilotFeatures.isGuidedModes() ? request.resolvedMode() : ChatMode.LIGHT_HINT;
    }
}

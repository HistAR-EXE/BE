package com.histar.be.rag.dto;

import com.histar.be.chat.dto.ChatHistoryTurn;
import com.histar.be.chat.dto.StationChatContext;
import com.histar.be.chat.knowledge.ChatKnowledgeIntent;
import com.histar.be.chat.knowledge.ChatKnowledgeRoute;
import java.util.List;
import java.util.Set;

public record RagChatRequest(
        String question,
        String stationCode,
        String siteCode,
        String personaKey,
        String personaPrompt,
        String locationName,
        List<ChatHistoryTurn> history,
        StationChatContext stationContext,
        ChatKnowledgeRoute knowledgeRoute) {

    public RagChatRequest {
        knowledgeRoute = knowledgeRoute == null ? legacyHeritageRoute() : knowledgeRoute;
    }

    public RagChatRequest(
            String question,
            String stationCode,
            String personaKey,
            String personaPrompt,
            String locationName,
            List<ChatHistoryTurn> history) {
        this(question, stationCode, null, personaKey, personaPrompt, locationName, history, null, null);
    }

    public RagChatRequest(
            String question,
            String stationCode,
            String siteCode,
            String personaKey,
            String personaPrompt,
            String locationName,
            List<ChatHistoryTurn> history) {
        this(question, stationCode, siteCode, personaKey, personaPrompt, locationName, history, null, null);
    }

    public RagChatRequest(
            String question,
            String stationCode,
            String siteCode,
            String personaKey,
            String personaPrompt,
            String locationName,
            List<ChatHistoryTurn> history,
            StationChatContext stationContext) {
        this(question, stationCode, siteCode, personaKey, personaPrompt, locationName, history, stationContext, null);
    }

    public boolean isHeritageOnly() {
        return knowledgeRoute.intent() == ChatKnowledgeIntent.HERITAGE_FACT
                && knowledgeRoute.scopes().equals(Set.of(ChatKnowledgeRoute.KnowledgeScope.HERITAGE));
    }

    private static ChatKnowledgeRoute legacyHeritageRoute() {
        return new ChatKnowledgeRoute(
                ChatKnowledgeIntent.HERITAGE_FACT,
                Set.of(ChatKnowledgeRoute.KnowledgeScope.HERITAGE),
                null,
                null,
                false,
                null);
    }
}

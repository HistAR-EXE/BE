package com.histar.be.rag.dto;

import com.histar.be.chat.dto.ChatHistoryTurn;
import com.histar.be.chat.dto.StationChatContext;
import java.util.List;

public record RagChatRequest(
        String question,
        String stationCode,
        String siteCode,
        String personaKey,
        String personaPrompt,
        String locationName,
        List<ChatHistoryTurn> history,
        StationChatContext stationContext) {
    public RagChatRequest(
            String question,
            String stationCode,
            String personaKey,
            String personaPrompt,
            String locationName,
            List<ChatHistoryTurn> history) {
        this(question, stationCode, null, personaKey, personaPrompt, locationName, history, null);
    }

    public RagChatRequest(
            String question,
            String stationCode,
            String siteCode,
            String personaKey,
            String personaPrompt,
            String locationName,
            List<ChatHistoryTurn> history) {
        this(question, stationCode, siteCode, personaKey, personaPrompt, locationName, history, null);
    }
}

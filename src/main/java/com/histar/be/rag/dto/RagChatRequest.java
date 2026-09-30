package com.histar.be.rag.dto;

import com.histar.be.chat.dto.ChatHistoryTurn;
import java.util.List;

public record RagChatRequest(
        String question,
        String stationCode,
        String siteCode,
        String personaKey,
        String personaPrompt,
        String locationName,
        List<ChatHistoryTurn> history) {
    public RagChatRequest(
            String question,
            String stationCode,
            String personaKey,
            String personaPrompt,
            String locationName,
            List<ChatHistoryTurn> history) {
        this(question, stationCode, null, personaKey, personaPrompt, locationName, history);
    }
}

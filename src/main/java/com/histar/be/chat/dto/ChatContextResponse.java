package com.histar.be.chat.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ChatContextResponse(
        UUID conversationId,
        String personaKey,
        Map<String, String> personaOverride,
        String knowledgeContext,
        String sources,
        UUID locationId,
        List<ChatHistoryTurn> history,
        Map<String, Object> playerContext) {}

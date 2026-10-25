package com.histar.be.chat.dto;

import com.histar.be.chat.knowledge.ChatKnowledgeIntent;
import java.util.List;
import java.util.UUID;

public record ChatResponse(
        String reply,
        UUID conversationId,
        List<ChatSource> sources,
        ChatKnowledgeIntent knowledgeIntent,
        boolean userStateGrounded) {

    public ChatResponse(
            String reply,
            UUID conversationId,
            List<ChatSource> sources,
            ChatKnowledgeIntent knowledgeIntent) {
        this(reply, conversationId, sources, knowledgeIntent, false);
    }

    public ChatResponse(String reply, UUID conversationId, List<ChatSource> sources) {
        this(reply, conversationId, sources, null, false);
    }
}

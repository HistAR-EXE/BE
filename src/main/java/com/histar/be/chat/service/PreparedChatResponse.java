package com.histar.be.chat.service;

import com.histar.be.chat.dto.ChatSource;
import com.histar.be.chat.knowledge.ChatKnowledgeIntent;
import java.util.List;
import java.util.UUID;

/** A generated answer that has not been written to conversation history yet. */
public record PreparedChatResponse(
        UUID conversationId,
        String reply,
        List<ChatSource> sources,
        String answerSource,
        boolean questHint,
        long startedNanos,
        ChatKnowledgeIntent knowledgeIntent,
        boolean userStateGrounded) {

    public PreparedChatResponse(
            UUID conversationId,
            String reply,
            List<ChatSource> sources,
            String answerSource,
            boolean questHint,
            long startedNanos,
            ChatKnowledgeIntent knowledgeIntent) {
        this(conversationId, reply, sources, answerSource, questHint, startedNanos, knowledgeIntent, false);
    }

    public PreparedChatResponse(
            UUID conversationId,
            String reply,
            List<ChatSource> sources,
            String answerSource,
            boolean questHint,
            long startedNanos) {
        this(conversationId, reply, sources, answerSource, questHint, startedNanos, null, false);
    }
}

package com.histar.be.chat.service;

import com.histar.be.chat.dto.ChatSource;
import java.util.List;
import java.util.UUID;

/** A generated answer that has not been written to conversation history yet. */
public record PreparedChatResponse(
        UUID conversationId, String reply, List<ChatSource> sources, String answerSource, boolean questHint, long startedNanos) {}

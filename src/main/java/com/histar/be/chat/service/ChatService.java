package com.histar.be.chat.service;

import com.histar.be.chat.dto.ChatContextResponse;
import com.histar.be.chat.dto.ChatMessageRequest;
import com.histar.be.chat.dto.ChatRequest;
import com.histar.be.chat.dto.ChatResponse;
import com.histar.be.chat.dto.ChatSyncRequest;
import com.histar.be.chat.dto.MessageResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChatService {

    ChatResponse chat(UUID userId, ChatRequest request);

    ChatContextResponse getContext(UUID userId, UUID characterId, UUID conversationId);

    ChatResponse sync(UUID userId, ChatSyncRequest request);

    /** BE-orchestrated: context → RAG AI :8100 → persist (legacy getContext/sync unchanged). */
    ChatResponse sendOrchestrated(UUID userId, ChatMessageRequest request);

    Page<MessageResponse> getMessages(UUID userId, UUID conversationId, Pageable pageable);
}

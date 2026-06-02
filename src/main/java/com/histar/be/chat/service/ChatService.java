package com.histar.be.chat.service;

import com.histar.be.chat.dto.ChatRequest;
import com.histar.be.chat.dto.ChatResponse;
import com.histar.be.chat.dto.MessageResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChatService {

    ChatResponse chat(UUID userId, ChatRequest request);

    Page<MessageResponse> getMessages(UUID userId, UUID conversationId, Pageable pageable);
}

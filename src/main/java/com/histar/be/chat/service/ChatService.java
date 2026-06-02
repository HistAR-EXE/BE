package com.histar.be.chat.service;

import com.histar.be.chat.dto.ChatRequest;
import com.histar.be.chat.dto.ChatResponse;
import com.histar.be.chat.dto.MessageResponse;
import java.util.List;
import java.util.UUID;

public interface ChatService {

    ChatResponse chat(UUID userId, ChatRequest request);

    List<MessageResponse> getMessages(UUID userId, UUID conversationId);
}

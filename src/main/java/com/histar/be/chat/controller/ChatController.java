package com.histar.be.chat.controller;

import com.histar.be.chat.dto.ChatRequest;
import com.histar.be.chat.dto.ChatResponse;
import com.histar.be.chat.dto.MessageResponse;
import com.histar.be.chat.service.ChatService;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping
    public ApiResponse<ChatResponse> chat(@RequestBody @Valid ChatRequest request) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        return ApiResponse.ok(chatService.chat(userId, request));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<List<MessageResponse>> getMessages(@PathVariable UUID conversationId) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        return ApiResponse.ok(chatService.getMessages(userId, conversationId));
    }
}

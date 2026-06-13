package com.histar.be.chat.controller;

import com.histar.be.chat.dto.ChatContextResponse;
import com.histar.be.chat.dto.ChatMessageRequest;
import com.histar.be.chat.dto.ChatRequest;
import com.histar.be.chat.dto.ChatResponse;
import com.histar.be.chat.dto.ChatSyncRequest;
import com.histar.be.chat.dto.MessageResponse;
import com.histar.be.chat.service.ChatService;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.response.PageResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/context")
    public ApiResponse<ChatContextResponse> getContext(
            @RequestParam UUID characterId, @RequestParam(required = false) UUID conversationId) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        return ApiResponse.ok(chatService.getContext(userId, characterId, conversationId));
    }

    /** BE-orchestrated chat (RAG AI). Legacy: GET /context + FE→AI + POST /sync. */
    @PostMapping("/messages")
    public ApiResponse<ChatResponse> sendMessage(@RequestBody @Valid ChatMessageRequest request) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        return ApiResponse.ok(chatService.sendOrchestrated(userId, request));
    }

    @PostMapping("/sync")
    public ApiResponse<ChatResponse> sync(@RequestBody @Valid ChatSyncRequest request) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        return ApiResponse.ok(chatService.sync(userId, request));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<PageResponse<MessageResponse>> getMessages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,asc") String sort) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        String[] sortParts = sort.split(",");
        String sortField = sortParts.length > 0 ? sortParts[0] : "createdAt";
        Sort.Direction direction =
                sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC;
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(direction, sortField));
        var result = chatService.getMessages(userId, conversationId, pageable);
        PageResponse<MessageResponse> body = PageResponse.<MessageResponse>builder()
                .items(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalItems(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
        return ApiResponse.ok(body);
    }
}

package com.histar.be.chat.controller;

import com.histar.be.chat.dto.ChatContextResponse;
import com.histar.be.chat.dto.ChatMessageRequest;
import com.histar.be.chat.dto.ChatRequest;
import com.histar.be.chat.dto.ChatResponse;
import com.histar.be.chat.dto.ChatSyncRequest;
import com.histar.be.chat.dto.MessageResponse;
import com.histar.be.chat.guided.GuidedChatRequest;
import com.histar.be.chat.guided.GuidedChatStreamService;
import com.histar.be.chat.service.ChatService;
import com.histar.be.chat.service.RagServiceException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.QuotaExceededException;
import com.histar.be.config.PilotFeatureProperties;
import com.histar.be.observability.ProductionMetrics;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.response.PageResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    public static final long SSE_TIMEOUT_MILLIS = 120_000L;

    private final ChatService chatService;
    private final GuidedChatStreamService guidedChatStreamService;
    private final ProductionMetrics productionMetrics;
    private final PilotFeatureProperties pilotFeatures;
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

    /** Progressive browser contract; the synchronous endpoint remains for existing clients. */
    @PostMapping(path = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@RequestBody @Valid GuidedChatRequest request) {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        if (!pilotFeatures.isChatStreaming()) {
            throw new BusinessRuleException("Chat streaming chưa được bật cho cohort này.");
        }
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        AtomicBoolean completed = new AtomicBoolean(false);
        Map<String, String> context = MDC.getCopyOfContextMap();
        String requestId = MDC.get("requestId");
        CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            try {
                if (context == null) MDC.clear(); else MDC.setContextMap(context);
                guidedChatStreamService.stream(userId, request, event -> {
                    if (completed.get()) {
                        throw new StreamDisconnectedException(new java.io.IOException("Stream closed"));
                    }
                    try {
                        emitter.send(SseEmitter.event().name(event.name()).data(event.data()));
                    } catch (java.io.IOException ex) {
                        throw new StreamDisconnectedException(ex);
                    }
                });
                if (completed.compareAndSet(false, true)) emitter.complete();
            } catch (StreamDisconnectedException ex) {
                if (completed.compareAndSet(false, true)) productionMetrics.recordChatStreamCancelled();
            } catch (Exception ex) {
                if (!completed.compareAndSet(false, true)) return;
                productionMetrics.recordChatStream(false);
                try {
                    emitter.send(SseEmitter.event().name("error").data(streamErrorPayload(ex)));
                } catch (Exception ignored) {
                    // Client may already have disconnected.
                }
                // The structured SSE error is the browser contract. Completing exceptionally can
                // terminate the HTTP response before the buffered error frame reaches fetch().
                emitter.complete();
            } finally {
                if (previous == null) MDC.clear(); else MDC.setContextMap(previous);
            }
        });
        emitter.onCompletion(() -> {
            if (completed.compareAndSet(false, true) && !task.isDone()) {
                task.cancel(true);
                productionMetrics.recordChatStreamCancelled();
            }
        });
        emitter.onTimeout(() -> {
            if (completed.compareAndSet(false, true)) {
                productionMetrics.recordChatStream(false);
                // Spring marks ResponseBodyEmitter complete before invoking timeout callbacks, so
                // this 120-second transport guard cannot legally write another SSE frame. The
                // typed RAG_TIMEOUT is emitted by the 100-second BE deadline while the stream is
                // still writable; this callback only cancels any straggling work and rejects late
                // events.
                task.cancel(true);
            }
        });
        return emitter;
    }

    private static Map<String, Object> streamErrorPayload(Exception exception) {
        if (exception instanceof RagServiceException rag) {
            return rag.payload();
        }
        if (exception instanceof QuotaExceededException quota) {
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("message", quota.getMessage());
            payload.put("code", quota.getErrorCode().getCode());
            payload.put("quotaType", quota.getQuotaType());
            payload.put("upgradeUrl", quota.getUpgradeUrl());
            if (quota.getUpgradePackage() != null) {
                payload.put("upgradePackage", quota.getUpgradePackage());
            }
            return payload;
        }
        return Map.of("message", "Không thể hoàn tất câu trả lời AI.", "code", "INTERNAL_ERROR");
    }

    private static final class StreamDisconnectedException extends RuntimeException {
        private StreamDisconnectedException(java.io.IOException cause) {
            super(cause);
        }
    }

    /** Records only browser capability fallback; never audio, transcript, or user identifiers. */
    @PostMapping("/telemetry/voice-fallback")
    public ApiResponse<Void> recordVoiceFallback() {
        currentUserAccessor.getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        productionMetrics.recordVoiceFallback();
        return ApiResponse.ok(null);
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

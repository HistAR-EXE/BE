package com.histar.be.config;

import com.histar.be.chat.controller.ChatController;
import com.histar.be.chat.guided.GuidedChatStreamService;
import com.histar.be.chat.service.AiResilienceExecutor;
import com.histar.be.chat.service.RagAiChatClient;
import com.histar.be.chat.service.RagServiceException;
import com.histar.be.chat.service.impl.ChatServiceImpl;
import com.histar.be.common.exception.GlobalExceptionHandler;
import com.histar.be.rag.config.RagProperties;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Effective non-secret chat configuration and an identity of the loaded chat bytecode. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRuntimeDiagnostics implements ApplicationRunner {
    private final ChatProperties chat;
    private final RagProperties rag;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("profiles", Arrays.stream(environment.getActiveProfiles())
                .map(ChatRuntimeDiagnostics::safeLabel).toList());
        summary.put("provider", safeLabel(environment.getProperty("chat.provider", "hybrid")));
        summary.put("ragOrigin", safeOrigin(chat.getRagAi().getBaseUrl()));
        summary.put("ragTimeoutSeconds", chat.getRagAi().getTimeoutSeconds());
        summary.put("ragMaxAttempts", AiResilienceExecutor.RAG_MAX_ATTEMPTS);
        summary.put("semanticRagEnabled", rag.isEnabled());
        summary.put("providerTimeoutMillis", chat.getResilience().getTimeout().toMillis());
        summary.put("providerMaxAttempts", chat.getResilience().getMaxAttempts());
        summary.put("circuitFailureThreshold", chat.getResilience().getFailureThreshold());
        summary.put("circuitOpenMillis", chat.getResilience().getOpenDuration().toMillis());
        summary.put("sseTimeoutMillis", ChatController.SSE_TIMEOUT_MILLIS);
        summary.put("chatBuildFingerprint", buildFingerprint());
        log.info("chat_runtime {}", summary);
    }

    private static String safeLabel(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{1,64}") ? value : "unknown";
    }

    private static String safeOrigin(String value) {
        try {
            URI uri = URI.create(value);
            if (!List.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null) return "invalid";
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), null, null, null).toString();
        } catch (Exception ignored) {
            return "invalid";
        }
    }

    private static String buildFingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Class<?> type : List.of(ChatRuntimeDiagnostics.class, ChatProperties.class, ChatController.class,
                    ChatServiceImpl.class, RagAiChatClient.class, RagServiceException.class,
                    AiResilienceExecutor.class, GuidedChatStreamService.class, GlobalExceptionHandler.class)) {
                try (var bytes = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
                    if (bytes == null) return "unavailable";
                    digest.update(bytes.readAllBytes());
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception ignored) {
            return "unavailable";
        }
    }
}

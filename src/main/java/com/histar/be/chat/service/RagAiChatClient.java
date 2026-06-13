package com.histar.be.chat.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.ChatProperties;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class RagAiChatClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient ragAiWebClient;
    private final int timeoutSeconds;

    public RagAiChatClient(ChatProperties chatProperties) {
        String baseUrl = chatProperties.getRagAi().getBaseUrl();
        this.timeoutSeconds = chatProperties.getRagAi().getTimeoutSeconds();
        this.ragAiWebClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public String generate(
            String message,
            String personaKey,
            Map<String, String> personaOverride,
            String knowledgeContext,
            String sources,
            UUID locationId,
            List<Map<String, String>> history,
            Integer userLevel,
            long artifactsUnlockedCount,
            long discoveriesCount) {
        return generate(message, personaKey, personaOverride, knowledgeContext, sources, locationId,
                history, userLevel, artifactsUnlockedCount, discoveriesCount, null);
    }

    public String generate(
            String message,
            String personaKey,
            Map<String, String> personaOverride,
            String knowledgeContext,
            String sources,
            UUID locationId,
            List<Map<String, String>> history,
            Integer userLevel,
            long artifactsUnlockedCount,
            long discoveriesCount,
            Map<String, Object> playerContext) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("persona_key", personaKey != null ? personaKey : "");
        body.put("persona_override", personaOverride != null ? personaOverride : Map.of());
        body.put("knowledge_context", knowledgeContext != null ? knowledgeContext : "");
        body.put("sources", sources != null ? sources : "");
        body.put("location_id", locationId != null ? locationId.toString() : "");
        body.put("history", history != null ? history : List.of());
        if (userLevel != null) {
            body.put("user_level", userLevel);
        }
        body.put("artifacts_unlocked_count", artifactsUnlockedCount);
        body.put("discoveries_count", discoveriesCount);
        if (playerContext != null && !playerContext.isEmpty()) {
            body.put("player_context", playerContext);
        }
        try {
            Map<String, Object> response = ragAiWebClient
                    .post()
                    .uri("/ai/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(MAP_TYPE)
                    .block(Duration.ofSeconds(timeoutSeconds));
            if (response == null) {
                throw new BusinessRuleException("RAG AI trả về response rỗng");
            }
            Object reply = response.get("reply");
            if (!(reply instanceof String s) || s.isBlank()) {
                throw new BusinessRuleException("RAG AI không trả lời được");
            }
            return s.trim();
        } catch (WebClientResponseException ex) {
            throw new BusinessRuleException("Gọi RAG AI thất bại: " + ex.getStatusCode());
        } catch (BusinessRuleException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessRuleException(
                    "RAG AI không khả dụng. Chạy: cd AI && uvicorn app.main:app --port 8100");
        }
    }
}

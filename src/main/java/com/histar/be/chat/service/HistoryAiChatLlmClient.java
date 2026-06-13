package com.histar.be.chat.service;

import com.histar.be.common.exception.BusinessRuleException;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class HistoryAiChatLlmClient implements ChatLlmClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient historyAiWebClient;
    private final boolean enabled;
    private final int timeoutSeconds;

    public HistoryAiChatLlmClient(
            WebClient historyAiWebClient,
            @Value("${chat.history-ai.enabled}") boolean enabled,
            @Value("${chat.history-ai.timeout-seconds}") int timeoutSeconds) {
        this.historyAiWebClient = historyAiWebClient;
        this.enabled = enabled;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String name() {
        return "history-ai";
    }

    @Override
    public boolean isAvailable() {
        if (!enabled) {
            return false;
        }
        try {
            historyAiWebClient
                    .get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(3));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public String generate(String prompt) {
        if (!enabled) {
            throw new BusinessRuleException("History AI service chưa bật (HISTORY_AI_ENABLED=false)");
        }
        Map<String, Object> body = Map.of("prompt", prompt);
        try {
            Map<String, Object> response = historyAiWebClient
                    .post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(MAP_TYPE)
                    .block(Duration.ofSeconds(timeoutSeconds));
            if (response == null) {
                throw new BusinessRuleException("History AI trả về response rỗng");
            }
            Object reply = response.get("reply");
            if (!(reply instanceof String s) || s.isBlank()) {
                throw new BusinessRuleException("History AI không trả lời được");
            }
            return s.trim();
        } catch (WebClientResponseException ex) {
            throw new BusinessRuleException("Gọi History AI thất bại: " + ex.getStatusCode());
        } catch (BusinessRuleException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessRuleException(
                    "History AI không khả dụng. Chạy: cd history-ai && uvicorn main:app --port 8000");
        }
    }
}

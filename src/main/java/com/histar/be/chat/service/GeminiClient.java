package com.histar.be.chat.service;

import com.histar.be.common.exception.BusinessRuleException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class GeminiClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient geminiWebClient;
    private final String apiKey;
    private final String model;

    public GeminiClient(
            WebClient geminiWebClient,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model) {
        this.geminiWebClient = geminiWebClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String generate(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessRuleException("GEMINI_API_KEY chưa được cấu hình");
        }
        Map<String, Object> body = Map.of(
                "contents",
                List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        try {
            Map<String, Object> response = geminiWebClient
                    .post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(MAP_TYPE)
                    .block();
            if (response == null) {
                throw new BusinessRuleException("Gemini trả về response rỗng");
            }
            String text = extractReplyText(response);
            if (text == null || text.isBlank()) {
                throw new BusinessRuleException("Gemini không trả lời được");
            }
            return text;
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().value() == 429) {
                throw new BusinessRuleException(
                        "Gemini quá tải (429). Đợi vài phút rồi thử lại — key vẫn hợp lệ.");
            }
            throw new BusinessRuleException("Gọi Gemini thất bại: " + ex.getStatusCode());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractReplyText(Map<String, Object> response) {
        Object candidatesObj = response.get("candidates");
        if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) {
            return null;
        }
        Object first = candidates.get(0);
        if (!(first instanceof Map<?, ?> candidate)) {
            return null;
        }
        Object contentObj = candidate.get("content");
        if (!(contentObj instanceof Map<?, ?> content)) {
            return null;
        }
        Object partsObj = content.get("parts");
        if (!(partsObj instanceof List<?> parts) || parts.isEmpty()) {
            return null;
        }
        Object part = parts.get(0);
        if (!(part instanceof Map<?, ?> partMap)) {
            return null;
        }
        Object text = partMap.get("text");
        return text instanceof String s ? s : null;
    }
}

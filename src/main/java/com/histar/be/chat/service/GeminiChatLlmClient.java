package com.histar.be.chat.service;

import com.histar.be.common.exception.BusinessRuleException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class GeminiChatLlmClient implements ChatLlmClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    /** gemini-2.0-* shut down 2026-06-01 and return 404. Try the configured model, then these. */
    private static final List<String> MODEL_FALLBACKS = List.of("gemini-2.5-flash", "gemini-3.1-flash-lite");

    private final WebClient geminiWebClient;
    private final String apiKey;
    private final String model;

    public GeminiChatLlmClient(
            WebClient geminiWebClient,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model}") String model) {
        this.geminiWebClient = geminiWebClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String name() {
        return "gemini";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String generate(String prompt) {
        if (!isAvailable()) {
            throw new BusinessRuleException("GEMINI_API_KEY chưa được cấu hình");
        }
        LinkedHashSet<String> models = new LinkedHashSet<>();
        if (model != null && !model.isBlank()) {
            models.add(model.trim());
        }
        models.addAll(MODEL_FALLBACKS);
        WebClientResponseException lastNotFound = null;
        for (String candidate : new ArrayList<>(models)) {
            try {
                return generateWithModel(prompt, candidate);
            } catch (WebClientResponseException ex) {
                if (ex.getStatusCode().value() == 404) {
                    lastNotFound = ex;
                    continue;
                }
                throw mapGeminiError(ex);
            }
        }
        throw mapGeminiError(lastNotFound);
    }

    private String generateWithModel(String prompt, String modelId) {
        Map<String, Object> body = Map.of(
                "contents",
                List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        Map<String, Object> response = geminiWebClient
                .post()
                .uri("/v1beta/models/{model}:generateContent", modelId)
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
    }

    private BusinessRuleException mapGeminiError(WebClientResponseException ex) {
        if (ex != null && ex.getStatusCode().value() == 429) {
            return new BusinessRuleException(
                    "Gemini quá tải (429). Đợi vài phút rồi thử lại — key vẫn hợp lệ.");
        }
        String status = ex == null ? "404 NOT_FOUND" : ex.getStatusCode().toString();
        return new BusinessRuleException("Gọi Gemini thất bại: " + status);
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

package com.histar.be.chat.service;

import com.histar.be.common.exception.BusinessRuleException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@Slf4j
public class GeminiChatLlmClient implements ChatLlmClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    /** Hết lượt (429) hoặc model bận thì thử tên kế tiếp. */
    private static final List<String> MODEL_FALLBACKS = List.of(
            "gemini-3.5-flash",
            "gemini-3.8-flash",
            "gemini-3.1-flash-lite",
            "gemini-3.1-flash-lite-preview",
            "gemini-3.7-flash",
            "gemini-3.5-flash-lite",
            "gemma-4-31b-it",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-3-flash");

    private final WebClient geminiWebClient;
    private final String apiKey;
    private final String model;
    private final List<String> modelFallbacks;
    private final AiResilienceExecutor resilience;

    public GeminiChatLlmClient(
            WebClient geminiWebClient,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model}") String model,
            @Value("${gemini.fallbacks:}") String fallbacks,
            AiResilienceExecutor resilience) {
        this.geminiWebClient = geminiWebClient;
        this.apiKey = apiKey;
        this.model = model;
        if (fallbacks == null || fallbacks.isBlank()) {
            this.modelFallbacks = MODEL_FALLBACKS;
        } else {
            this.modelFallbacks = List.of(fallbacks.split(",")).stream()
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .toList();
        }
        this.resilience = resilience;
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
        models.addAll(modelFallbacks);
        RuntimeException lastError = null;
        for (String candidate : new ArrayList<>(models)) {
            try {
                String reply = generateWithModel(prompt, candidate);
                log.info("gemini_model_ok model={}", candidate);
                return reply;
            } catch (WebClientResponseException ex) {
                int code = ex.getStatusCode().value();
                if (code == 404 || code == 400 || code == 429 || code == 503) {
                    lastError = ex;
                    log.info("gemini_model_skip model={} error_type=http_{}", candidate, code);
                    continue;
                }
                throw mapGeminiError(ex);
            } catch (AiServiceUnavailableException | AiCircuitOpenException ex) {
                lastError = ex;
                log.info("gemini_model_skip model={} error_type=resilience", candidate);
            }
        }
        if (lastError instanceof WebClientResponseException responseException) {
            throw mapGeminiError(responseException);
        }
        throw lastError == null ? new BusinessRuleException("Gemini không khả dụng") : lastError;
    }

    private String generateWithModel(String prompt, String modelId) {
        Map<String, Object> body = Map.of(
                "contents",
                List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        Map<String, Object> response = resilience.execute("gemini", () -> geminiWebClient
                .post()
                .uri("/v1beta/models/{model}:generateContent", modelId)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(MAP_TYPE)
                .block());
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

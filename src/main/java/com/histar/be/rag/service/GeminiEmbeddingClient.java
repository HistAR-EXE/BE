package com.histar.be.rag.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.rag.config.RagProperties;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Gemini {@code embedContent} client (REST, reuses the shared {@code geminiWebClient} + GEMINI_API_KEY). */
@Component
public class GeminiEmbeddingClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    public static final String TASK_QUERY = "RETRIEVAL_QUERY";
    public static final String TASK_DOCUMENT = "RETRIEVAL_DOCUMENT";

    private final WebClient geminiWebClient;
    private final String apiKey;
    private final RagProperties ragProperties;

    public GeminiEmbeddingClient(
            @Qualifier("geminiWebClient") WebClient geminiWebClient,
            @Value("${gemini.api-key:}") String apiKey,
            RagProperties ragProperties) {
        this.geminiWebClient = geminiWebClient;
        this.apiKey = apiKey;
        this.ragProperties = ragProperties;
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Embeds a user question (RETRIEVAL_QUERY). */
    public float[] embedQuery(String text) {
        return embed(text, TASK_QUERY);
    }

    public float[] embed(String text, String taskType) {
        if (!isAvailable()) {
            throw new BusinessRuleException("GEMINI_API_KEY chưa được cấu hình");
        }
        if (text == null || text.isBlank()) {
            throw new BusinessRuleException("Nội dung embedding rỗng");
        }
        RagProperties.Embedding cfg = ragProperties.getEmbedding();
        String model = cfg.getModel();
        Map<String, Object> body = new HashMap<>();
        body.put("model", "models/" + model);
        body.put("content", Map.of("parts", List.of(Map.of("text", text))));
        body.put("taskType", taskType);
        if (cfg.getDimensions() > 0) {
            body.put("outputDimensionality", cfg.getDimensions());
        }
        Map<String, Object> response;
        try {
            response = geminiWebClient
                    .post()
                    .uri("/v1beta/models/{model}:embedContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(MAP_TYPE)
                    .block(Duration.ofSeconds(Math.max(1, cfg.getTimeoutSeconds())));
        } catch (WebClientResponseException ex) {
            throw new BusinessRuleException("Gọi Gemini embedding thất bại: " + ex.getStatusCode());
        }
        float[] vector = extractValues(response);
        if (cfg.getDimensions() > 0 && vector.length != cfg.getDimensions()) {
            throw new BusinessRuleException("Embedding có " + vector.length + " chiều, cần " + cfg.getDimensions());
        }
        return vector;
    }

    static float[] extractValues(Map<String, Object> response) {
        if (response == null || !(response.get("embedding") instanceof Map<?, ?> embedding)) {
            throw new BusinessRuleException("Gemini embedding trả về response rỗng");
        }
        if (!(embedding.get("values") instanceof List<?> values) || values.isEmpty()) {
            throw new BusinessRuleException("Gemini embedding không có giá trị");
        }
        float[] out = new float[values.size()];
        for (int i = 0; i < out.length; i++) {
            if (!(values.get(i) instanceof Number n)) {
                throw new BusinessRuleException("Gemini embedding có giá trị không hợp lệ");
            }
            out[i] = n.floatValue();
        }
        return out;
    }
}

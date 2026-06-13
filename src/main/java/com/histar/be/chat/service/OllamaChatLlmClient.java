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
public class OllamaChatLlmClient implements ChatLlmClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient ollamaWebClient;
    private final String model;
    private final int timeoutSeconds;

    public OllamaChatLlmClient(
            WebClient ollamaWebClient,
            @Value("${chat.ollama.model}") String model,
            @Value("${chat.ollama.timeout-seconds}") int timeoutSeconds) {
        this.ollamaWebClient = ollamaWebClient;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String name() {
        return "ollama";
    }

    @Override
    public boolean isAvailable() {
        try {
            ollamaWebClient
                    .get()
                    .uri("/api/tags")
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
        Map<String, Object> body =
                Map.of("model", model, "prompt", prompt, "stream", false);
        try {
            Map<String, Object> response = ollamaWebClient
                    .post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(MAP_TYPE)
                    .block(Duration.ofSeconds(timeoutSeconds));
            if (response == null) {
                throw new BusinessRuleException("Ollama trả về response rỗng");
            }
            Object text = response.get("response");
            if (!(text instanceof String s) || s.isBlank()) {
                throw new BusinessRuleException("Ollama không trả lời được");
            }
            return s.trim();
        } catch (WebClientResponseException ex) {
            throw new BusinessRuleException("Gọi Ollama thất bại: " + ex.getStatusCode());
        } catch (BusinessRuleException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessRuleException(
                    "Ollama không khả dụng. Chạy: ollama serve && ollama pull " + model);
        }
    }
}

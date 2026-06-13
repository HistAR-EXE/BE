package com.histar.be.chat.config;

import com.histar.be.chat.service.ChatLlmClient;
import com.histar.be.chat.service.GeminiChatLlmClient;
import com.histar.be.chat.service.HistoryAiChatLlmClient;
import com.histar.be.chat.service.HybridChatLlmClient;
import com.histar.be.chat.service.OllamaChatLlmClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ChatLlmConfig {

    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder().baseUrl("https://generativelanguage.googleapis.com").build();
    }

    @Bean
    public WebClient ollamaWebClient(@Value("${chat.ollama.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient historyAiWebClient(@Value("${chat.history-ai.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    @Primary
    public ChatLlmClient chatLlmClient(
            @Value("${chat.provider}") String provider,
            @Value("${chat.hybrid-order}") String hybridOrder,
            GeminiChatLlmClient gemini,
            OllamaChatLlmClient ollama,
            HistoryAiChatLlmClient historyAi) {
        Map<String, ChatLlmClient> byName =
                Map.of("gemini", gemini, "ollama", ollama, "history-ai", historyAi);
        return switch (provider.toLowerCase(Locale.ROOT)) {
            case "gemini" -> gemini;
            case "ollama" -> ollama;
            case "history-ai" -> historyAi;
            default -> new HybridChatLlmClient(resolveHybridOrder(hybridOrder, byName));
        };
    }

    private List<ChatLlmClient> resolveHybridOrder(String hybridOrder, Map<String, ChatLlmClient> byName) {
        List<ChatLlmClient> ordered = Arrays.stream(hybridOrder.split(","))
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .map(byName::get)
                .filter(client -> client != null)
                .collect(Collectors.toCollection(ArrayList::new));
        if (ordered.isEmpty()) {
            ordered.addAll(byName.values());
        }
        return ordered;
    }
}

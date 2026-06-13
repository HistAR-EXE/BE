package com.histar.be.chat.service;

import com.histar.be.common.exception.BusinessRuleException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HybridChatLlmClient implements ChatLlmClient {

    private static final Logger log = LoggerFactory.getLogger(HybridChatLlmClient.class);

    private final List<ChatLlmClient> providers;

    public HybridChatLlmClient(List<ChatLlmClient> providers) {
        this.providers = List.copyOf(providers);
    }

    @Override
    public String name() {
        return "hybrid";
    }

    @Override
    public boolean isAvailable() {
        return providers.stream().anyMatch(ChatLlmClient::isAvailable);
    }

    @Override
    public String generate(String prompt) {
        Map<String, String> failures = new LinkedHashMap<>();
        for (ChatLlmClient provider : providers) {
            if (!provider.isAvailable()) {
                failures.put(provider.name(), "not available");
                continue;
            }
            try {
                String reply = provider.generate(prompt);
                log.info("Chat reply from provider: {}", provider.name());
                return reply;
            } catch (Exception ex) {
                failures.put(provider.name(), ex.getMessage());
                log.warn("Chat provider {} failed: {}", provider.name(), ex.getMessage());
            }
        }
        throw new BusinessRuleException(buildFailureMessage(failures));
    }

    private String buildFailureMessage(Map<String, String> failures) {
        if (failures.isEmpty()) {
            return "Không có AI provider nào được cấu hình. Bật History AI / Ollama hoặc set GEMINI_API_KEY.";
        }
        StringBuilder message = new StringBuilder("Tất cả AI providers đều thất bại. ");
        failures.forEach((provider, reason) ->
                message.append('[').append(provider).append(": ").append(reason).append("] "));
        message.append(
                "Gợi ý: chạy history-ai (port 8000), ollama serve, hoặc set GEMINI_API_KEY trong .env rồi restart BE.");
        return message.toString().trim();
    }
}

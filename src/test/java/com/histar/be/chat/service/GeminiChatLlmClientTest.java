package com.histar.be.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeminiChatLlmClientTest {

    @Test
    void extracts_text_from_a_gemini_stream_chunk() {
        Map<String, Object> chunk = Map.of(
                "candidates", List.of(Map.of(
                        "content", Map.of("parts", List.of(Map.of("text", "Xin chào"))))));

        assertThat(GeminiChatLlmClient.extractReplyTextFromStreamChunk(chunk)).isEqualTo("Xin chào");
    }

    @Test
    void ignores_non_text_stream_chunks() {
        assertThat(GeminiChatLlmClient.extractReplyTextFromStreamChunk(Map.of("candidates", List.of())))
                .isNull();
    }
}

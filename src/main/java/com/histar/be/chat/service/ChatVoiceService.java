package com.histar.be.chat.service;

import com.histar.be.config.ChatProperties;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ChatVoiceService {
    private final URI endpoint;
    // Uvicorn's HTTP/1.1 endpoint does not support Java's default clear-text h2c upgrade.
    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5)).build();
    private final JsonMapper mapper = JsonMapper.builder().build();

    public ChatVoiceService(ChatProperties properties) {
        endpoint = URI.create(properties.getRagAi().getBaseUrl().replaceAll("/+$", "") + "/ai/voice/tts");
    }

    public record SpeechAudio(byte[] bytes, String contentType) {}

    public SpeechAudio synthesize(String text, String personaKey) {
        try {
            var body = mapper.writeValueAsString(Map.of("text", text,
                    "persona_key", personaKey == null ? "chrono" : personaKey));
            var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 || response.body().length == 0
                    || !response.headers().firstValue("Content-Type").orElse("").startsWith("audio/")) {
                throw new RagServiceException(false, true, MDC.get("requestId"));
            }
            return new SpeechAudio(response.body(), response.headers().firstValue("Content-Type").orElse("audio/mpeg"));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RagServiceException(false, true, MDC.get("requestId"));
        } catch (java.net.http.HttpTimeoutException ex) {
            throw new RagServiceException(true, true, MDC.get("requestId"));
        } catch (RagServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RagServiceException(false, true, MDC.get("requestId"));
        }
    }
}

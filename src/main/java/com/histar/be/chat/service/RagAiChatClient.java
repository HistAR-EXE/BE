package com.histar.be.chat.service;

import com.histar.be.config.ChatProperties;
import com.histar.be.chat.dto.ChatSource;
import com.histar.be.chat.dto.StationChatContext;
import com.histar.be.chat.knowledge.ChatKnowledgeRoute;
import com.histar.be.chat.knowledge.ChatPageContext;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.slf4j.MDC;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Component
@Slf4j
public class RagAiChatClient {

    private final HttpClient httpClient;
    private final URI chatUri;
    private final Duration requestTimeout;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final AiResilienceExecutor resilience;

    public RagAiChatClient(ChatProperties chatProperties, AiResilienceExecutor resilience) {
        String baseUrl = chatProperties.getRagAi().getBaseUrl().replaceAll("/+$", "");
        this.chatUri = URI.create(baseUrl + "/ai/chat");
        this.requestTimeout = Duration.ofSeconds(chatProperties.getRagAi().getTimeoutSeconds());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(requestTimeout)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.resilience = resilience;
    }

    public String generate(
            String message,
            String personaKey,
            Map<String, String> personaOverride,
            String knowledgeContext,
            String sources,
            UUID locationId,
            List<Map<String, String>> history,
            Integer userLevel,
            long artifactsUnlockedCount,
            long discoveriesCount) {
        return generateWithSources(
                        message,
                        personaKey,
                        personaOverride,
                        knowledgeContext,
                        sources,
                        locationId,
                        history,
                        userLevel,
                        artifactsUnlockedCount,
                        discoveriesCount,
                        null)
                .reply();
    }

    public RagChatResponse generateWithSources(
            String message,
            String personaKey,
            Map<String, String> personaOverride,
            String knowledgeContext,
            String sources,
            UUID locationId,
            List<Map<String, String>> history,
            Integer userLevel,
            long artifactsUnlockedCount,
            long discoveriesCount) {
        return generateWithSources(
                message,
                personaKey,
                personaOverride,
                knowledgeContext,
                sources,
                locationId,
                history,
                userLevel,
                artifactsUnlockedCount,
                discoveriesCount,
                null);
    }

    public RagChatResponse generateWithSources(
            String message,
            String personaKey,
            Map<String, String> personaOverride,
            String knowledgeContext,
            String sources,
            UUID locationId,
            List<Map<String, String>> history,
            Integer userLevel,
            long artifactsUnlockedCount,
            long discoveriesCount,
            Map<String, Object> playerContext) {
        return generateWithSources(
                message, personaKey, personaOverride, knowledgeContext, sources, locationId, history,
                userLevel, artifactsUnlockedCount, discoveriesCount, playerContext, null);
    }

    public RagChatResponse generateWithSources(
            String message,
            String personaKey,
            Map<String, String> personaOverride,
            String knowledgeContext,
            String sources,
            UUID locationId,
            List<Map<String, String>> history,
            Integer userLevel,
            long artifactsUnlockedCount,
            long discoveriesCount,
            Map<String, Object> playerContext,
            StationChatContext stationContext) {
        return generateWithSources(
                message, personaKey, personaOverride, knowledgeContext, sources, locationId, history,
                userLevel, artifactsUnlockedCount, discoveriesCount, playerContext, stationContext, null);
    }

    public RagChatResponse generateWithSources(
            String message,
            String personaKey,
            Map<String, String> personaOverride,
            String knowledgeContext,
            String sources,
            UUID locationId,
            List<Map<String, String>> history,
            Integer userLevel,
            long artifactsUnlockedCount,
            long discoveriesCount,
            Map<String, Object> playerContext,
            StationChatContext stationContext,
            ChatKnowledgeRoute knowledgeRoute) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("persona_key", personaKey != null ? personaKey : "");
        body.put("persona_override", personaOverride != null ? personaOverride : Map.of());
        body.put("knowledge_context", knowledgeContext != null ? knowledgeContext : "");
        body.put("sources", sources != null ? sources : "");
        body.put("location_id", locationId != null ? locationId.toString() : "");
        body.put("history", history != null ? history : List.of());
        if (userLevel != null) {
            body.put("user_level", userLevel);
        }
        body.put("artifacts_unlocked_count", artifactsUnlockedCount);
        body.put("discoveries_count", discoveriesCount);
        if (playerContext != null && !playerContext.isEmpty()) {
            body.put("player_context", playerContext);
        }
        if (stationContext != null) {
            body.put("station_context", Map.of(
                    "siteCode", stationContext.siteCode(),
                    "stationCode", stationContext.stationCode(),
                    "stationName", stationContext.stationName(),
                    "summary", stationContext.summary() == null ? "" : stationContext.summary()));
        }
        if (knowledgeRoute != null) {
            body.put("knowledge_intent", knowledgeRoute.intent().name());
            body.put("knowledge_scopes", knowledgeRoute.scopes().stream()
                    .sorted(java.util.Comparator.comparingInt(Enum::ordinal))
                    .map(Enum::name)
                    .toList());
            if (knowledgeRoute.pageContext() != null || knowledgeRoute.featureKey() != null) {
                Map<String, Object> page = knowledgeRoute.pageContext() == null
                        ? new LinkedHashMap<>() : pageContextPayload(knowledgeRoute.pageContext());
                // The feature being asked about can differ from the page currently open.
                if (knowledgeRoute.featureKey() != null) {
                    page.put("requestedFeatureKey", knowledgeRoute.featureKey());
                }
                body.put("page_context", page);
            }
        }
        String requestId = MDC.get("requestId");
        if (requestId == null || requestId.isBlank()) requestId = UUID.randomUUID().toString();
        final String correlationId = requestId;
        try {
            Map<String, Object> response = resilience.execute("rag-ai", () -> executeRequest(body, correlationId));
            if (response == null) {
                throw new RagServiceException(false, true, correlationId);
            }
            Object reply = response.get("reply");
            if (!(reply instanceof String s) || s.isBlank()) {
                throw new RagServiceException(false, true, correlationId);
            }
            Object cache = response.get("cache");
            String cacheName = cache instanceof String value ? value : "";
            return new RagChatResponse(s.trim(), parseSources(response.get("sources")), cacheName);
        } catch (RagServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            RagServiceException failure = mapFailure(ex, correlationId);
            // Never log upstream bodies, prompts, URLs, credentials or exception messages.
            log.warn("requestId={} dependency=rag-ai code={} retryable={}",
                    correlationId, failure.getErrorCode().getCode(), failure.isRetryable());
            throw failure;
        }
    }

    private static Map<String, Object> pageContextPayload(ChatPageContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (context.featureKey() != null) payload.put("featureKey", context.featureKey());
        if (context.route() != null) payload.put("route", context.route());
        if (context.siteCode() != null) payload.put("siteCode", context.siteCode());
        if (context.stationCode() != null) payload.put("stationCode", context.stationCode());
        return payload;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeRequest(Map<String, Object> body, String requestId)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(chatUri)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-Request-Id", requestId)
                .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RagHttpException(response.statusCode(), response.body());
        }
        Object decoded = jsonMapper.readValue(response.body(), Map.class);
        if (!(decoded instanceof Map<?, ?> map)) {
            throw new IOException("Invalid RAG response envelope");
        }
        return (Map<String, Object>) map;
    }

    private static RagServiceException mapFailure(Throwable failure, String requestId) {
        boolean timeout = false;
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.util.concurrent.TimeoutException
                    || cause instanceof java.net.SocketTimeoutException
                    || cause instanceof HttpTimeoutException) timeout = true;
            if (cause instanceof RagHttpException response) {
                boolean upstreamTimeout = response.statusCode == 504;
                boolean retryable = true;
                try {
                    var detail = JsonMapper.builder().build().readTree(response.responseBody).path("detail");
                    String code = detail.path("code").asText();
                    if ("RAG_TIMEOUT".equals(code) || "RAG_UNAVAILABLE".equals(code)) {
                        upstreamTimeout = "RAG_TIMEOUT".equals(code);
                        retryable = detail.path("retryable").asBoolean(true);
                    }
                } catch (RuntimeException ignored) {
                    // Non-contract gateway responses still map to a safe typed failure.
                }
                return new RagServiceException(upstreamTimeout, retryable, requestId);
            }
            if (cause instanceof WebClientResponseException response) {
                boolean upstreamTimeout = response.getStatusCode().value() == 504;
                boolean retryable = true;
                try {
                    var detail = JsonMapper.builder().build().readTree(response.getResponseBodyAsString()).path("detail");
                    String code = detail.path("code").asText();
                    if ("RAG_TIMEOUT".equals(code) || "RAG_UNAVAILABLE".equals(code)) {
                        upstreamTimeout = "RAG_TIMEOUT".equals(code);
                        retryable = detail.path("retryable").asBoolean(true);
                    }
                } catch (RuntimeException ignored) {
                    // Non-contract gateway responses still map to a safe typed failure.
                }
                return new RagServiceException(upstreamTimeout, retryable, requestId);
            }
        }
        return new RagServiceException(timeout, true, requestId);
    }

    private static final class RagHttpException extends IOException {
        private final int statusCode;
        private final String responseBody;

        private RagHttpException(int statusCode, String responseBody) {
            super("RAG HTTP " + statusCode);
            this.statusCode = statusCode;
            this.responseBody = responseBody == null ? "" : responseBody;
        }
    }

    @SuppressWarnings("unchecked")
    private List<ChatSource> parseSources(Object raw) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<ChatSource> out = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Object title = map.get("title");
            Object excerpt = map.get("excerpt");
            Object url = map.get("url");
            Object scope = map.get("scope");
            if (!(title instanceof String titleStr) || titleStr.isBlank()) {
                continue;
            }
            String excerptStr = excerpt instanceof String s && !s.isBlank() ? s : titleStr;
            String urlStr = url instanceof String u && !u.isBlank() ? u : null;
            String scopeStr = scope instanceof String s && !s.isBlank() ? s.trim() : null;
            out.add(new ChatSource(titleStr.trim(), excerptStr.trim(), urlStr, scopeStr));
        }
        return out;
    }
}

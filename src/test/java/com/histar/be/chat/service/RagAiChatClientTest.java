package com.histar.be.chat.service;

import static org.assertj.core.api.Assertions.*;

import com.histar.be.common.exception.BaseException;
import com.histar.be.common.exception.GlobalExceptionHandler;
import com.histar.be.config.ChatProperties;
import com.histar.be.chat.knowledge.ChatKnowledgeIntent;
import com.histar.be.chat.knowledge.ChatKnowledgeRoute;
import com.histar.be.chat.knowledge.ChatPageContext;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.MDC;
import tools.jackson.databind.json.JsonMapper;

class RagAiChatClientTest {
    HttpServer server;
    AiResilienceExecutor resilience;
    RagAiChatClient client;
    AtomicInteger calls = new AtomicInteger();
    AtomicReference<String> header = new AtomicReference<>();
    AtomicReference<String> requestBody = new AtomicReference<>();
    int status = 200;
    String body = "{\"reply\":\"answer\",\"sources\":[],\"cache\":\"llm\"}";
    long delayMillis;

    @BeforeEach void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ai/chat", exchange -> {
            calls.incrementAndGet();
            header.set(exchange.getRequestHeaders().getFirst("X-Request-Id"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try { Thread.sleep(delayMillis); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            try {
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } finally { exchange.close(); }
        });
        server.start();
        var properties = new ChatProperties();
        properties.getRagAi().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.getRagAi().setTimeoutSeconds(1);
        properties.getResilience().setInitialBackoff(Duration.ZERO);
        resilience = new AiResilienceExecutor(properties);
        client = new RagAiChatClient(properties, resilience);
        MDC.put("requestId", "be-request-123");
    }

    @AfterEach void cleanup() {
        resilience.close();
        server.stop(0);
        MDC.clear();
    }

    RagChatResponse call() {
        return client.generateWithSources("question", "chrono", Map.of(), "", "", null, List.of(), 1, 0, 0);
    }

    @ParameterizedTest
    @CsvSource({"503,RAG_UNAVAILABLE", "504,RAG_TIMEOUT"})
    void mapsStructuredUpstreamFailureToPublicStatusWithoutRetry(int upstreamStatus, String code) {
        status = upstreamStatus;
        body = "{\"detail\":{\"code\":\"" + code + "\",\"message\":\"safe upstream message\",\"retryable\":false,\"requestId\":\"be-request-123\"}}";
        var exception = catchThrowable(this::call);
        assertThat(exception).isInstanceOf(BaseException.class);
        var response = new GlobalExceptionHandler().handleBaseException((BaseException) exception);
        assertThat(response.getStatusCode().value()).isEqualTo(upstreamStatus);
        var json = JsonMapper.builder().build().valueToTree(response.getBody());
        assertThat(json.path("code").asText()).isEqualTo(code);
        assertThat(json.path("retryable").asBoolean(true)).isFalse();
        assertThat(json.path("requestId").asText()).isEqualTo("be-request-123");
        assertThat(calls).hasValue(1);
    }

    @Test void forwardsRequestIdAndAcceptsAdditionalDiagnostics() {
        body = "{\"reply\":\"answer\",\"sources\":[],\"diagnostics\":{\"corpusFingerprint\":\"c1\",\"configFingerprint\":\"f1\",\"model\":\"model\",\"sourceIds\":[\"s1\"]}}";
        assertThat(call().reply()).isEqualTo("answer");
        assertThat(header.get()).isEqualTo("be-request-123");
    }

    @Test void repeatedNoSourceResponsesCompleteWithoutTransportStateLeak() {
        body = "{\"reply\":\"Tư liệu hiện có chưa đủ.\",\"sources\":[],\"cache\":\"no_source\"}";
        for (int index = 0; index < 20; index++) {
            assertThat(call().reply()).contains("chưa đủ");
        }
        assertThat(calls).hasValue(20);
    }

    @Test void deadlineReturnsRetryable504WithRequestId() {
        delayMillis = 1600;
        var exception = catchThrowable(this::call);
        assertThat(exception).isInstanceOf(BaseException.class);
        var response = new GlobalExceptionHandler().handleBaseException((BaseException) exception);
        assertThat(response.getStatusCode().value()).isEqualTo(504);
        var json = JsonMapper.builder().build().valueToTree(response.getBody());
        assertThat(json.path("code").asText()).isEqualTo("RAG_TIMEOUT");
        assertThat(json.path("retryable").asBoolean()).isTrue();
        assertThat(json.path("requestId").asText()).isEqualTo("be-request-123");
        assertThat(calls).hasValue(1);
    }

    @Test void malformedSuccessIsRetryableUnavailable() {
        body = "{}";
        var exception = catchThrowable(this::call);
        assertThat(exception).isInstanceOf(BaseException.class);
        var response = new GlobalExceptionHandler().handleBaseException((BaseException) exception);
        assertThat(response.getStatusCode().value()).isEqualTo(503);
    }

    @Test void sendsScopedKnowledgeAndNavigationContextWithoutDroppingLiveState() throws Exception {
        var pageContext = new ChatPageContext(
                "STATION_MEDIA", "/sites/cu-chi/stations/ST01/media", "cu-chi", "ST01");
        var route = new ChatKnowledgeRoute(
                ChatKnowledgeIntent.MIXED,
                Set.of(ChatKnowledgeRoute.KnowledgeScope.PLATFORM,
                        ChatKnowledgeRoute.KnowledgeScope.HERITAGE),
                "STATION_MEDIA",
                pageContext,
                false,
                null);
        var playerContext = Map.<String, Object>of("remainingQuests", 2);
        var stationContext = new com.histar.be.chat.dto.StationChatContext(
                "cu-chi", "ST01", "Phòng họp", "Tóm tắt đã xác minh");

        client.generateWithSources(
                "question", "chrono", Map.of(), "", "", null, List.of(),
                1, 0, 0, playerContext, stationContext, route);

        var json = JsonMapper.builder().build().readTree(requestBody.get());
        assertThat(json.path("knowledge_intent").asText()).isEqualTo("MIXED");
        assertThat(json.path("knowledge_scopes").toString()).isEqualTo("[\"PLATFORM\",\"HERITAGE\"]");
        assertThat(json.path("page_context").path("featureKey").asText()).isEqualTo("STATION_MEDIA");
        assertThat(json.path("page_context").path("route").asText())
                .isEqualTo("/sites/cu-chi/stations/ST01/media");
        assertThat(json.path("station_context").path("stationCode").asText()).isEqualTo("ST01");
        assertThat(json.path("player_context").path("remainingQuests").asInt()).isEqualTo(2);
    }

    @Test void sendsRequestedFeatureEvenWhenChatPageHasADifferentFeature() throws Exception {
        var route = new ChatKnowledgeRoute(ChatKnowledgeIntent.PRODUCT_HELP,
                Set.of(ChatKnowledgeRoute.KnowledgeScope.PLATFORM), "BADGES",
                new ChatPageContext("CHAT_AI", "/chat", "cu-chi", null), false, null);
        client.generateWithSources("Có bao nhiêu huy hiệu?", "chrono", Map.of(), "", "", null,
                List.of(), 1, 0, 0, Map.of(), null, route);
        var json = JsonMapper.builder().build().readTree(requestBody.get());
        assertThat(json.path("page_context").path("featureKey").asText()).isEqualTo("CHAT_AI");
        assertThat(json.path("page_context").path("requestedFeatureKey").asText()).isEqualTo("BADGES");
    }

    @Test void sendsRequestedFeatureWithoutAnyPageContext() throws Exception {
        var route = new ChatKnowledgeRoute(ChatKnowledgeIntent.PRODUCT_HELP,
                Set.of(ChatKnowledgeRoute.KnowledgeScope.PLATFORM), "BADGES", null, false, null);
        client.generateWithSources("Có bao nhiêu huy hiệu?", "chrono", Map.of(), "", "", null,
                List.of(), 1, 0, 0, Map.of(), null, route);
        var json = JsonMapper.builder().build().readTree(requestBody.get());
        assertThat(json.path("page_context").path("requestedFeatureKey").asText()).isEqualTo("BADGES");
    }

    @Test void parsesOptionalSourceScopeAndKeepsLegacySourcesCompatible() {
        body = """
                {"reply":"answer","sources":[
                  {"title":"Hướng dẫn sử dụng TimeLens · Cổ vật","excerpt":"Cách dùng","scope":"PLATFORM"},
                  {"title":"Nguồn lịch sử","excerpt":"Tư liệu"}
                ]}
                """;

        var sources = call().sources();

        assertThat(sources).hasSize(2);
        assertThat(sources.get(0).scope()).isEqualTo("PLATFORM");
        assertThat(sources.get(1).scope()).isNull();
    }
}

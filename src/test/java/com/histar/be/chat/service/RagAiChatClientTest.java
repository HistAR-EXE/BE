package com.histar.be.chat.service;

import static org.assertj.core.api.Assertions.*;

import com.histar.be.common.exception.BaseException;
import com.histar.be.common.exception.GlobalExceptionHandler;
import com.histar.be.config.ChatProperties;
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
    int status = 200;
    String body = "{\"reply\":\"answer\",\"sources\":[],\"cache\":\"llm\"}";
    long delayMillis;

    @BeforeEach void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ai/chat", exchange -> {
            calls.incrementAndGet();
            header.set(exchange.getRequestHeaders().getFirst("X-Request-Id"));
            exchange.getRequestBody().readAllBytes();
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
}

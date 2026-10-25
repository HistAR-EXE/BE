package com.histar.be.chat.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.histar.be.chat.guided.GuidedChatStreamService;
import com.histar.be.chat.dto.ChatMessageRequest;
import com.histar.be.chat.service.ChatService;
import com.histar.be.chat.service.RagServiceException;
import com.histar.be.common.exception.GlobalExceptionHandler;
import com.histar.be.common.exception.QuotaExceededException;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.common.web.RequestTracingFilter;
import com.histar.be.config.PilotFeatureProperties;
import com.histar.be.observability.ProductionMetrics;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ChatControllerRagContractTest {
    ChatService chat = mock(ChatService.class);
    GuidedChatStreamService streaming = mock(GuidedChatStreamService.class);
    ProductionMetrics metrics = mock(ProductionMetrics.class);
    CurrentUserAccessor users = mock(CurrentUserAccessor.class);
    MockMvc mvc;
    String request = "{\"characterId\":\"" + UUID.randomUUID() + "\",\"message\":\"question\"}";

    @BeforeEach void setup() {
        when(users.getUserId()).thenReturn(Optional.of(UUID.randomUUID()));
        var flags = new PilotFeatureProperties();
        flags.setChatStreaming(true);
        mvc = MockMvcBuilders.standaloneSetup(new ChatController(chat, streaming, metrics, flags, users))
                .setControllerAdvice(new GlobalExceptionHandler()).addFilters(new RequestTracingFilter()).build();
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void synchronousErrorsHaveTypedStatusAndCorrelation(boolean timeout) throws Exception {
        when(chat.sendOrchestrated(any(), any())).thenAnswer(invocation -> {
            throw new RagServiceException(timeout, true, MDC.get("requestId"));
        });
        mvc.perform(post("/api/chat/messages").header("X-Request-Id", "sync-123")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().is(timeout ? 504 : 503))
                .andExpect(jsonPath("$.code").value(timeout ? "RAG_TIMEOUT" : "RAG_UNAVAILABLE"))
                .andExpect(jsonPath("$.retryable").value(true))
                .andExpect(jsonPath("$.requestId").value("sync-123"))
                .andExpect(header().string("X-Request-Id", "sync-123"));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void streamingErrorsKeepTypedCodeRequestIdAnd120SecondBudget(boolean timeout) throws Exception {
        var workerId = new AtomicReference<String>();
        doAnswer(invocation -> {
            workerId.set(MDC.get("requestId"));
            throw new RagServiceException(timeout, true, "stream-123");
        }).when(streaming).stream(any(), any(), any());
        var result = mvc.perform(post("/api/chat/messages/stream").header("X-Request-Id", "stream-123")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(request().asyncStarted()).andReturn();
        result.getAsyncResult(5000);
        String content = mvc.perform(asyncDispatch(result)).andReturn().getResponse().getContentAsString();
        assertThat(content).contains("event:error", timeout ? "RAG_TIMEOUT" : "RAG_UNAVAILABLE", "\"retryable\":true", "\"requestId\":\"stream-123\"")
                .doesNotContain("event:complete");
        assertThat(workerId.get()).isEqualTo("stream-123");
        assertThat(result.getRequest().getAsyncContext().getTimeout()).isEqualTo(120_000);
        verify(metrics, never()).recordChatStream(true);
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test void quotaErrorStillContainsUpgradeDetails() throws Exception {
        doThrow(new QuotaExceededException("quota", "/upgrade", "B2C_DAILY", "PREMIUM"))
                .when(streaming).stream(any(), any(), any());
        var result = mvc.perform(post("/api/chat/messages/stream").contentType(MediaType.APPLICATION_JSON).content(request))
                .andReturn();
        result.getAsyncResult(5000);
        String content = mvc.perform(asyncDispatch(result)).andReturn().getResponse().getContentAsString();
        assertThat(content).contains("QUOTA_EXCEEDED", "B2C_DAILY", "/upgrade", "PREMIUM").doesNotContain("event:complete");
    }

    @Test void messageRequestAcceptsLegacyJsonWithoutPageContext() throws Exception {
        mvc.perform(post("/api/chat/messages").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(chat).sendOrchestrated(any(), captor.capture());
        assertThat(captor.getValue().pageContext()).isNull();
    }

    @Test void messageRequestBindsBoundedPageContext() throws Exception {
        String body = """
                {"characterId":"%s","message":"question","pageContext":{
                  "featureKey":"ARTIFACTS","route":"/collection","siteCode":"cu-chi","stationCode":"ST01"}}
                """.formatted(UUID.randomUUID());

        mvc.perform(post("/api/chat/messages").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(ChatMessageRequest.class);
        verify(chat).sendOrchestrated(any(), captor.capture());
        assertThat(captor.getValue().pageContext().featureKey()).isEqualTo("ARTIFACTS");
        assertThat(captor.getValue().pageContext().stationCode()).isEqualTo("ST01");
    }

    @Test void messageRequestRejectsOversizedPageContext() throws Exception {
        String body = """
                {"characterId":"%s","message":"question","pageContext":{
                  "featureKey":"%s","route":"/collection"}}
                """.formatted(UUID.randomUUID(), "A".repeat(65));

        mvc.perform(post("/api/chat/messages").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(chat);
    }

    @Test void servletTimeoutRejectsLateStreamEventsWithoutFalseCompletion() throws Exception {
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var finished = new java.util.concurrent.CountDownLatch(1);
        var lateAccepted = new java.util.concurrent.atomic.AtomicBoolean();
        doAnswer(invocation -> {
            try {
                java.util.function.Consumer<com.histar.be.chat.guided.GuidedChatStreamEvent> sink = invocation.getArgument(2);
                entered.countDown();
                release.await(5, java.util.concurrent.TimeUnit.SECONDS);
                sink.accept(new com.histar.be.chat.guided.GuidedChatStreamEvent("delta", "late answer"));
                lateAccepted.set(true);
            } finally { finished.countDown(); }
            return null;
        }).when(streaming).stream(any(), any(), any());
        var result = mvc.perform(post("/api/chat/messages/stream").header("X-Request-Id", "timeout-123")
                .contentType(MediaType.APPLICATION_JSON).content(request)).andReturn();
        try {
            assertThat(entered.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var async = (org.springframework.mock.web.MockAsyncContext) result.getRequest().getAsyncContext();
            for (var listener : async.getListeners()) listener.onTimeout(new jakarta.servlet.AsyncEvent(async));
            release.countDown();
            assertThat(finished.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            result.getAsyncResult(5000);
            var response = mvc.perform(asyncDispatch(result)).andReturn().getResponse();
            assertThat(response.getContentAsString()).doesNotContain("late answer", "event:complete");
            assertThat(lateAccepted).isFalse();
            verify(metrics).recordChatStream(false);
        } finally { release.countDown(); }
    }
}

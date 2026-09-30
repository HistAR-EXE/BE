package com.histar.be.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.chat.service.ChatLlmClient;
import com.histar.be.rag.config.RagProperties;
import com.histar.be.rag.dto.RagChatAnswer;
import com.histar.be.rag.dto.RagChatRequest;
import com.histar.be.rag.dto.RetrievalResult;
import com.histar.be.rag.dto.RetrievedChunk;
import com.histar.be.rag.entity.ChatQualityLog;
import com.histar.be.rag.repository.ChatQualityLogRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RagChatServiceTest {

    @Mock
    private PgVectorRagService ragService;

    @Mock
    private SemanticCacheService semanticCache;

    @Mock
    private ChatLlmClient chatLlmClient;

    @Mock
    private ChatQualityLogRepository qualityLogRepository;

    private RagProperties props;
    private RagChatService service;

    @BeforeEach
    void setUp() {
        props = new RagProperties();
        props.setEnabled(true);
        service = new RagChatService(ragService, semanticCache, chatLlmClient, qualityLogRepository, props);
    }

    private static RetrievedChunk chunk(String title, String content) {
        return new RetrievedChunk(UUID.randomUUID(), UUID.randomUUID(), content, "ST03", "1946-1975", 0, title, "cc", 0.2);
    }

    private static RagChatRequest request() {
        return new RagChatRequest("Bếp Hoàng Cầm là gì?", "ST03", "chi-nam", "Bạn là Chị Năm.", "Củ Chi", List.of());
    }

    @Test
    void disabled_returnsEmptyAndDoesNothing() {
        props.setEnabled(false);

        assertThat(service.tryAnswer(request())).isEmpty();
        verify(ragService, never()).retrieve(anyString(), any());
    }

    @Test
    void noChunks_returnsEmptyForLegacyFallback_andLogsQuality() {
        when(ragService.retrieve(anyString(), any())).thenReturn(new RetrievalResult(List.of(), "[0]"));

        assertThat(service.tryAnswer(request())).isEmpty();

        ArgumentCaptor<ChatQualityLog> log = ArgumentCaptor.forClass(ChatQualityLog.class);
        verify(qualityLogRepository).save(log.capture());
        assertThat(log.getValue().getChunksUsed()).isZero();
        assertThat(log.getValue().isHadCitation()).isFalse();
        assertThat(log.getValue().getQuestionHash()).hasSize(64);
        verify(chatLlmClient, never()).generate(anyString());
    }

    @Test
    void citedReply_returnsOnlyCitedSources_andStoresCache() {
        List<RetrievedChunk> chunks = List.of(chunk("Nguồn A", "Nội dung A"), chunk("Nguồn B", "Nội dung B"));
        when(ragService.retrieve(anyString(), any())).thenReturn(new RetrievalResult(chunks, "[1,2]"));
        when(semanticCache.lookup(any(), any(), anyString())).thenReturn(Optional.empty());
        when(chatLlmClient.generate(anyString())).thenReturn("Bếp giảm khói [2] và kín đáo [9].");

        RagChatAnswer answer = service.tryAnswer(request()).orElseThrow();

        assertThat(answer.refused()).isFalse();
        assertThat(answer.reply()).isEqualTo("Bếp giảm khói [2] và kín đáo .");
        assertThat(answer.sources()).hasSize(1);
        assertThat(answer.sources().get(0).title()).isEqualTo("Nguồn B");
        verify(semanticCache).store("[1,2]", "ST03", "chi-nam", answer.reply());

        ArgumentCaptor<ChatQualityLog> log = ArgumentCaptor.forClass(ChatQualityLog.class);
        verify(qualityLogRepository).save(log.capture());
        assertThat(log.getValue().getChunksUsed()).isEqualTo(2);
        assertThat(log.getValue().isHadCitation()).isTrue();
    }

    @Test
    void promptContainsNumberedSourcesAndQuestion() {
        List<RetrievedChunk> chunks = List.of(chunk("Nguồn A", "Nội dung A"), chunk("Nguồn B", "Nội dung B"));
        when(ragService.retrieve(anyString(), any())).thenReturn(new RetrievalResult(chunks, "[1]"));
        when(semanticCache.lookup(any(), any(), anyString())).thenReturn(Optional.empty());
        when(chatLlmClient.generate(anyString())).thenReturn("ok [1]");

        service.tryAnswer(request());

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(chatLlmClient).generate(prompt.capture());
        assertThat(prompt.getValue())
                .contains("[1] Nguồn A")
                .contains("[2] Nguồn B")
                .contains("Câu hỏi của người dùng: Bếp Hoàng Cầm là gì?")
                .contains("Bạn là Chị Năm.");
    }

    @Test
    void uncitedReply_isSoftRefused_andNotCached() {
        when(ragService.retrieve(anyString(), any()))
                .thenReturn(new RetrievalResult(List.of(chunk("Nguồn A", "Nội dung A")), "[1]"));
        when(semanticCache.lookup(any(), any(), anyString())).thenReturn(Optional.empty());
        when(chatLlmClient.generate(anyString())).thenReturn("Câu trả lời không có chú thích nguồn.");

        RagChatAnswer answer = service.tryAnswer(request()).orElseThrow();

        assertThat(answer.refused()).isTrue();
        assertThat(answer.reply()).isEqualTo(RagChatService.SOFT_REFUSE_REPLY);
        assertThat(answer.sources()).isEmpty();
        verify(semanticCache, never()).store(any(), any(), any(), any());

        ArgumentCaptor<ChatQualityLog> log = ArgumentCaptor.forClass(ChatQualityLog.class);
        verify(qualityLogRepository).save(log.capture());
        assertThat(log.getValue().isHadCitation()).isFalse();
    }

    @Test
    void cacheHit_skipsLlm() {
        List<RetrievedChunk> chunks = List.of(chunk("Nguồn A", "Nội dung A"));
        when(ragService.retrieve(anyString(), any())).thenReturn(new RetrievalResult(chunks, "[1]"));
        when(semanticCache.lookup(any(), any(), anyString())).thenReturn(Optional.of("Đã cache [1]"));

        RagChatAnswer answer = service.tryAnswer(request()).orElseThrow();

        assertThat(answer.fromCache()).isTrue();
        assertThat(answer.sources()).hasSize(1);
        verify(chatLlmClient, never()).generate(anyString());
    }

    @Test
    void retrievalFailure_fallsBackToLegacy() {
        when(ragService.retrieve(anyString(), any())).thenThrow(new IllegalStateException("embedding down"));

        assertThat(service.tryAnswer(request())).isEmpty();
    }
}

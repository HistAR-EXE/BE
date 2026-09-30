package com.histar.be.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.rag.config.RagProperties;
import com.histar.be.rag.dto.RetrievalResult;
import com.histar.be.rag.repository.RagChunkRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PgVectorRagServiceTest {

    @Mock
    private RagChunkRepository chunkRepository;

    @Mock
    private GeminiEmbeddingClient embeddingClient;

    private RagProperties props;
    private PgVectorRagService service;

    @BeforeEach
    void setUp() {
        props = new RagProperties();
        props.setEnabled(true);
        props.setTopK(3);
        props.setMaxDistance(0.5);
        service = new PgVectorRagService(chunkRepository, embeddingClient, props);
    }

    private static Object[] row(String content, String station, double distance) {
        return new Object[] {
            UUID.randomUUID(), UUID.randomUUID(), content, station, "1946-1975", 0, "Nguồn mẫu", "internal", distance
        };
    }

    @Test
    void disabled_returnsEmptyWithoutCallingEmbedding() {
        props.setEnabled(false);

        RetrievalResult result = service.retrieve("Bếp Hoàng Cầm là gì?", "ST03");

        assertThat(result.chunks()).isEmpty();
        verify(embeddingClient, never()).embedQuery(anyString());
    }

    @Test
    void stationHitsAreUsed_andGlobalSearchSkipped() {
        when(embeddingClient.isAvailable()).thenReturn(true);
        when(embeddingClient.embedQuery("q")).thenReturn(new float[] {0.1f, 0.2f});
        when(chunkRepository.searchByStation(anyString(), eq("ST03"), eq("cu-chi"), eq(3)))
                .thenReturn(List.<Object[]>of(row("a", "ST03", 0.2), row("b", "ST03", 0.4)));

        RetrievalResult result = service.retrieve("q", "ST03", "cu-chi");

        assertThat(result.chunks()).extracting("content").containsExactly("a", "b");
        assertThat(result.vectorLiteral()).isEqualTo("[0.1000000,0.2000000]");
        verify(chunkRepository, never()).searchGlobal(anyString(), anyString(), anyInt());
    }

    @Test
    void fallsBackToGlobal_whenStationHasNothingWithinThreshold() {
        when(embeddingClient.isAvailable()).thenReturn(true);
        when(embeddingClient.embedQuery("q")).thenReturn(new float[] {1f});
        when(chunkRepository.searchByStation(anyString(), eq("ST03"), eq("cu-chi"), eq(3)))
                .thenReturn(List.<Object[]>of(row("far", "ST03", 0.9)));
        when(chunkRepository.searchGlobal(anyString(), eq("cu-chi"), eq(3)))
                .thenReturn(List.<Object[]>of(row("global-near", null, 0.3), row("global-far", "ST01", 0.7)));

        RetrievalResult result = service.retrieve("q", "ST03", "cu-chi");

        assertThat(result.chunks()).extracting("content").containsExactly("global-near");
    }

    @Test
    void noStationCode_searchesGlobalOnly() {
        when(embeddingClient.isAvailable()).thenReturn(true);
        when(embeddingClient.embedQuery("q")).thenReturn(new float[] {1f});
        when(chunkRepository.searchGlobal(anyString(), isNull(), eq(3)))
                .thenReturn(List.<Object[]>of(row("g", null, 0.1)));

        RetrievalResult result = service.retrieve("q", "  ");

        assertThat(result.chunks()).hasSize(1);
        verify(chunkRepository, never()).searchByStation(anyString(), anyString(), anyString(), anyInt());
    }
}

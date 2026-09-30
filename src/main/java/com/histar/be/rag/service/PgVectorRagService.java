package com.histar.be.rag.service;

import com.histar.be.rag.config.RagProperties;
import com.histar.be.rag.dto.RetrievalResult;
import com.histar.be.rag.dto.RetrievedChunk;
import com.histar.be.rag.repository.RagChunkRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Cosine-distance retrieval over verified {@code rag_chunks}: station-scoped first, global fallback when the
 * station has no chunk within {@code rag.max-distance}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PgVectorRagService {

    private final RagChunkRepository ragChunkRepository;
    private final GeminiEmbeddingClient embeddingClient;
    private final RagProperties ragProperties;

    public boolean isEnabled() {
        return ragProperties.isEnabled() && embeddingClient.isAvailable();
    }

    public RetrievalResult retrieve(String question, String stationCode) {
        return retrieve(question, stationCode, null);
    }

    public RetrievalResult retrieve(String question, String stationCode, String siteCode) {
        if (!isEnabled() || question == null || question.isBlank()) {
            return RetrievalResult.empty();
        }
        String vec = VectorUtils.toLiteral(embeddingClient.embedQuery(question));
        int k = Math.max(1, ragProperties.getTopK());
        String site = siteCode == null || siteCode.isBlank() ? null : siteCode.trim().toLowerCase();

        List<RetrievedChunk> hits = List.of();
        if (stationCode != null && !stationCode.isBlank()) {
            hits = withinThreshold(ragChunkRepository.searchByStation(vec, stationCode.trim(), site, k));
        }
        if (hits.isEmpty()) {
            hits = withinThreshold(ragChunkRepository.searchGlobal(vec, site, k));
        }
        return new RetrievalResult(hits, vec);
    }

    private List<RetrievedChunk> withinThreshold(List<Object[]> rows) {
        double max = ragProperties.getMaxDistance();
        return rows.stream().map(PgVectorRagService::toChunk).filter(c -> c.distance() <= max).toList();
    }

    static RetrievedChunk toChunk(Object[] r) {
        return new RetrievedChunk(
                toUuid(r[0]),
                toUuid(r[1]),
                (String) r[2],
                (String) r[3],
                (String) r[4],
                r[5] == null ? 0 : ((Number) r[5]).intValue(),
                (String) r[6],
                (String) r[7],
                ((Number) r[8]).doubleValue());
    }

    private static UUID toUuid(Object o) {
        return o instanceof UUID u ? u : UUID.fromString(String.valueOf(o));
    }
}

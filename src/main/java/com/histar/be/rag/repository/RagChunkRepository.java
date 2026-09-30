package com.histar.be.rag.repository;

import com.histar.be.rag.entity.RagChunk;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RagChunkRepository extends JpaRepository<RagChunk, UUID> {

    /**
     * Column order of rows returned by the search queries (keep in sync with {@code PgVectorRagService}):
     * 0 id, 1 source_id, 2 content, 3 station_code, 4 era, 5 chunk_index, 6 source title, 7 license, 8 cosine distance.
     * Only chunks with an embedding whose source is verified are searchable.
     */
    @Query(
            nativeQuery = true,
            value =
                    """
                    SELECT c.id, c.source_id, c.content, c.station_code, c.era, c.chunk_index,
                           s.title, s.license, (c.embedding <=> CAST(:vec AS vector)) AS distance
                    FROM rag_chunks c
                    JOIN rag_sources s ON s.id = c.source_id
                    WHERE c.embedding IS NOT NULL
                      AND s.verified_at IS NOT NULL
                      AND c.station_code = :stationCode
                      AND (CAST(:siteCode AS text) IS NULL OR c.site_code = :siteCode OR c.site_code IS NULL)
                    ORDER BY c.embedding <=> CAST(:vec AS vector)
                    LIMIT :k
                    """)
    List<Object[]> searchByStation(
            @Param("vec") String vec,
            @Param("stationCode") String stationCode,
            @Param("siteCode") String siteCode,
            @Param("k") int k);

    @Query(
            nativeQuery = true,
            value =
                    """
                    SELECT c.id, c.source_id, c.content, c.station_code, c.era, c.chunk_index,
                           s.title, s.license, (c.embedding <=> CAST(:vec AS vector)) AS distance
                    FROM rag_chunks c
                    JOIN rag_sources s ON s.id = c.source_id
                    WHERE c.embedding IS NOT NULL
                      AND s.verified_at IS NOT NULL
                      AND (CAST(:siteCode AS text) IS NULL OR c.site_code = :siteCode OR c.site_code IS NULL)
                    ORDER BY c.embedding <=> CAST(:vec AS vector)
                    LIMIT :k
                    """)
    List<Object[]> searchGlobal(
            @Param("vec") String vec, @Param("siteCode") String siteCode, @Param("k") int k);
}

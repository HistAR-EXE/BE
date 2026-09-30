package com.histar.be.rag.repository;

import com.histar.be.rag.entity.SemanticCacheEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SemanticCacheRepository extends JpaRepository<SemanticCacheEntry, UUID> {

    /** Rows: 0 reply_text, 1 cosine distance. {@code stationCode} is "" for global (NULL) entries. */
    @Query(
            nativeQuery = true,
            value =
                    """
                    SELECT reply_text, (embedding <=> CAST(:vec AS vector)) AS distance
                    FROM semantic_cache
                    WHERE embedding IS NOT NULL
                      AND expires_at > NOW()
                      AND persona = :persona
                      AND COALESCE(station_code, '') = :stationCode
                    ORDER BY embedding <=> CAST(:vec AS vector)
                    LIMIT 1
                    """)
    List<Object[]> findNearest(
            @Param("vec") String vec, @Param("persona") String persona, @Param("stationCode") String stationCode);

    @Transactional
    @Modifying
    @Query(
            nativeQuery = true,
            value =
                    """
                    INSERT INTO semantic_cache (id, embedding, station_code, persona, reply_text, expires_at)
                    VALUES (:id, CAST(:vec AS vector), :stationCode, :persona, :replyText, :expiresAt)
                    """)
    int insert(
            @Param("id") UUID id,
            @Param("vec") String vec,
            @Param("stationCode") String stationCode,
            @Param("persona") String persona,
            @Param("replyText") String replyText,
            @Param("expiresAt") Instant expiresAt);
}

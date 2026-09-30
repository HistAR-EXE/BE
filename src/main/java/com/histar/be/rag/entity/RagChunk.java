package com.histar.be.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A retrievable text chunk. The {@code embedding vector(768)} column is intentionally NOT mapped: it is written by
 * the ingest script and read only through native pgvector SQL ({@code RagChunkRepository}), which keeps H2-based
 * tests and Hibernate schema validation free of pgvector types.
 */
@Entity
@Table(name = "rag_chunks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "station_code", length = 32)
    private String stationCode;

    @Column(name = "site_code", length = 64)
    private String siteCode;

    @Column(length = 64)
    private String era;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;
}

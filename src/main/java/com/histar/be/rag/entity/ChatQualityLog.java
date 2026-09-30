package com.histar.be.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_quality_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatQualityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "question_hash", nullable = false, length = 64)
    private String questionHash;

    @Column(name = "station_code", length = 32)
    private String stationCode;

    @Column(name = "chunks_used", nullable = false)
    private int chunksUsed;

    @Column(name = "had_citation", nullable = false)
    private boolean hadCitation;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

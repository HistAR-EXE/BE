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

/** Semantic cache row; the {@code embedding} column is only touched through native SQL (see repository). */
@Entity
@Table(name = "semantic_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemanticCacheEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "station_code", length = 32)
    private String stationCode;

    @Column(nullable = false, length = 64)
    private String persona;

    @Column(name = "reply_text", nullable = false, columnDefinition = "TEXT")
    private String replyText;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}

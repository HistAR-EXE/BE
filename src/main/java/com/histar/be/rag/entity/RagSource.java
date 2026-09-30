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
@Table(name = "rag_sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 128)
    private String license;

    @Column(name = "verified_by", length = 128)
    private String verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(length = 64)
    private String era;

    @Column(name = "station_code", length = 32)
    private String stationCode;

    @Column(name = "site_code", length = 64)
    private String siteCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

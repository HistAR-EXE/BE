package com.histar.be.quest.narrative.entity;

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
@Table(name = "narrative_investigation_versions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NarrativeInvestigationVersion {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "investigation_id", nullable = false) private UUID investigationId;
    @Column(nullable = false) private int version;
    @Column(name = "graph_json", nullable = false, columnDefinition = "text") private String graphJson;
    @Column(name = "review_status", nullable = false) private String reviewStatus;
    @Column(name = "approved_locales_json", nullable = false, columnDefinition = "text") private String approvedLocalesJson;
    @Column(name = "reviewed_by") private String reviewedBy;
    @Column(name = "reviewed_at") private Instant reviewedAt;
    @Column(nullable = false) private long revision;
    @Column(name = "open_draft", nullable = false) private boolean openDraft;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}

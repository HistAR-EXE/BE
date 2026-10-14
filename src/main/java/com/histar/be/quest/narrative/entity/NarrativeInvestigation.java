package com.histar.be.quest.narrative.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "narrative_investigations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NarrativeInvestigation {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "quest_id", nullable = false, unique = true) private UUID questId;
    @Column(nullable = false) private int version;
    @Column(nullable = false) private boolean enabled;
    @Column(name = "graph_json", nullable = false, columnDefinition = "text") private String graphJson;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "review_status", nullable = false) private String reviewStatus;
    @Column(name = "approved_locales_json", nullable = false, columnDefinition = "text") private String approvedLocalesJson;
    @Column(name = "reviewed_by") private String reviewedBy;
    @Column(name = "reviewed_at") private Instant reviewedAt;
    @Column(name = "published_version_id") private UUID publishedVersionId;
    @Enumerated(EnumType.STRING) @Column(name = "rollout_state", nullable = false)
    @Builder.Default private NarrativeRolloutState rolloutState = NarrativeRolloutState.NOT_RELEASED;
    @Column(name = "row_version", nullable = false) private long rowVersion;
}

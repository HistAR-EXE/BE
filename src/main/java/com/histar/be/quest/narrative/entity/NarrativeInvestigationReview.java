package com.histar.be.quest.narrative.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "narrative_investigation_reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NarrativeInvestigationReview {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "investigation_id", nullable = false) private UUID investigationId;
    @Column(name = "version_id") private UUID versionId;
    @Column(name = "version") private Integer version;
    @Column(name = "revision") private Long revision;
    @Column(name = "audit_event_id") private UUID auditEventId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NarrativeReviewDecision decision;
    @Column(name = "approved_locales_json", nullable = false, columnDefinition = "text") private String approvedLocalesJson;
    @Column(name = "review_note", columnDefinition = "text") private String reviewNote;
    @Column(name = "reviewed_by", nullable = false) private String reviewedBy;
    @Column(name = "reviewed_at", nullable = false) private Instant reviewedAt;
}

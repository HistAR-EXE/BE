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
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NarrativeReviewDecision decision;
    @Column(name = "approved_locales_json", nullable = false, columnDefinition = "text") private String approvedLocalesJson;
    @Column(name = "review_note", columnDefinition = "text") private String reviewNote;
    @Column(name = "reviewed_by", nullable = false) private String reviewedBy;
    @Column(name = "reviewed_at", nullable = false) private Instant reviewedAt;
}

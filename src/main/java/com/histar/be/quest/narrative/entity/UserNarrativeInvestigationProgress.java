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
@Table(name = "user_narrative_investigation_progress")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserNarrativeInvestigationProgress {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "investigation_id", nullable = false) private UUID investigationId;
    @Column(name = "state_json", nullable = false, columnDefinition = "text") private String stateJson;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}

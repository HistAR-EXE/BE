package com.histar.be.quest.narrative.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "narrative_rollout_idempotency")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class NarrativeRolloutIdempotency {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "investigation_id", nullable = false) private UUID investigationId;
    @Column(name = "idempotency_key", nullable = false) private String idempotencyKey;
    @Column(nullable = false) private String action;
    @Column(name = "request_hash", nullable = false) private String requestHash;
    @Column(name = "response_json", nullable = false, columnDefinition = "text") private String responseJson;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}

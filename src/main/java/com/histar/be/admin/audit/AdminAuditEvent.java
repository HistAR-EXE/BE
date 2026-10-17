package com.histar.be.admin.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "admin_audit_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAuditEvent {
    @Id private UUID id;
    @Column(name = "actor_id") private UUID actorId;
    @Column(name = "entity_type", nullable = false) private String entityType;
    @Column(name = "entity_id", nullable = false) private UUID entityId;
    @Column(nullable = false) private String action;
    @Column(name = "old_row_version") private Long oldRowVersion;
    @Column(name = "new_row_version") private Long newRowVersion;
    @Column(columnDefinition = "text") private String reason;
    @Column(name = "idempotency_key") private String idempotencyKey;
    @Column(name = "metadata_json", nullable = false, columnDefinition = "text") private String metadataJson;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "event_hash", length = 64) private String eventHash;
    @Column(name = "prev_event_hash", length = 64) private String prevEventHash;
}

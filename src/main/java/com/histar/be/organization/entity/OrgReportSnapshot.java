package com.histar.be.organization.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "org_report_snapshots")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OrgReportSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "snapshot_json", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String snapshotJson;
}

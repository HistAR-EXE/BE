package com.histar.be.siteworkspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "site_content_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteContentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "site_code", nullable = false, length = 64)
    private String siteCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_kind", nullable = false, length = 32)
    private SiteContentKind contentKind;

    @Column(name = "resource_key", nullable = false, length = 128)
    private String resourceKey;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "station_id")
    private UUID stationId;

    @Column(nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private ContentLifecycleStatus status = ContentLifecycleStatus.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "jsonb")
    private String snapshotJson;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private long revision = 0;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "submitted_by")
    private UUID submittedBy;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "published_by")
    private UUID publishedBy;

    @Column(name = "review_note", columnDefinition = "text")
    private String reviewNote;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @PreUpdate
    void refreshUpdatedAt() {
        updatedAt = Instant.now();
    }
}

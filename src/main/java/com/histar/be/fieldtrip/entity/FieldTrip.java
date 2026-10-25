package com.histar.be.fieldtrip.entity;

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

@Entity
@Table(name = "field_trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "site_code", nullable = false, length = 64)
    private String siteCode;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "visit_start", nullable = false)
    private Instant visitStart;

    @Column(name = "visit_end", nullable = false)
    private Instant visitEnd;

    @Column(name = "quest_id", nullable = false)
    private UUID questId;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "join_code", nullable = false, length = 32)
    private String joinCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private FieldTripStatus status = FieldTripStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "site_confirmation_status", nullable = false, length = 32)
    @Builder.Default
    private SiteConfirmationStatus siteConfirmationStatus = SiteConfirmationStatus.NOT_REQUIRED;

    @Column(name = "site_note", columnDefinition = "text")
    private String siteNote;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    @PreUpdate
    void refreshUpdatedAt() {
        updatedAt = Instant.now();
    }
}

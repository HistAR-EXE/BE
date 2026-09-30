package com.histar.be.billing.entity;

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
@Table(name = "heritage_digitization_inquiries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeritageDigitizationInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "site_name", nullable = false)
    private String siteName;

    /** Pilot interest: cu-chi | hoang-thanh-thang-long | dai-noi-hue */
    @Column(name = "interest_site_code", length = 64)
    private String interestSiteCode;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "package_type", nullable = false, length = 32)
    private String packageType;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "NEW";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "contacted_at")
    private Instant contactedAt;
}

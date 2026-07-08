package com.histar.be.organization.entity;

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
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String slug;
    private String name;
    private String plan;

    @Column(name = "plan_type")
    private String planType;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "plan_start_date")
    private java.time.LocalDate planStartDate;

    @Column(name = "plan_end_date")
    private java.time.LocalDate planEndDate;

    private String status;

    @Column(name = "max_ccu")
    private Integer maxCcu;

    @Column(name = "max_verified_accounts")
    private Integer maxVerifiedAccounts;

    @Column(name = "max_ai_queries_per_month")
    private Integer maxAiQueriesPerMonth;

    @Column(name = "invite_code")
    private String inviteCode;

    @Column(name = "invite_code_expires_at")
    private Instant inviteCodeExpiresAt;

    private Instant createdAt;
}

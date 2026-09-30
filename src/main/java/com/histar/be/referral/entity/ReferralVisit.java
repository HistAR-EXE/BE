package com.histar.be.referral.entity;

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
@Table(name = "referral_visits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "referral_code", nullable = false, length = 64)
    private String referralCode;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "visited_at", nullable = false)
    @Builder.Default
    private Instant visitedAt = Instant.now();
}

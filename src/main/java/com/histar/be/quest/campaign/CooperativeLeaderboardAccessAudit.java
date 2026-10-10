package com.histar.be.quest.campaign;

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

@Entity
@Table(name = "cooperative_leaderboard_access_audit")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CooperativeLeaderboardAccessAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "actor_id")
    private UUID actorId;
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;
    @Column(nullable = false)
    private boolean allowed;
    @Column(nullable = false, length = 80)
    private String outcome;
    @Column(nullable = false)
    private Instant occurredAt;
    @Column(columnDefinition = "text")
    private String detail;
}

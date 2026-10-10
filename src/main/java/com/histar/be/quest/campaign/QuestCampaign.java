package com.histar.be.quest.campaign;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quest_campaigns")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QuestCampaign {
    @Id private UUID id;
    private UUID questId;
    private String campaignType;
    private String title;
    private Instant startsAt;
    private Instant endsAt;
    private boolean enabled;
    @Enumerated(EnumType.STRING)
    @Column(name = "pilot_status", nullable = false)
    @Builder.Default
    private PilotStatus pilotStatus = PilotStatus.PENDING;
    @Enumerated(EnumType.STRING)
    @Column(name = "release_decision", nullable = false)
    @Builder.Default
    private ReleaseDecision releaseDecision = ReleaseDecision.PENDING;
    @Column(name = "release_note", columnDefinition = "text")
    private String releaseNote;
    @Column(name = "release_decided_by")
    private UUID releaseDecidedBy;
    @Column(name = "release_decided_at")
    private Instant releaseDecidedAt;
}

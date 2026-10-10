package com.histar.be.quest.campaign;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID; import lombok.*;
@Entity @Table(name="quest_campaign_audit") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuestCampaignAudit {
    @Id @GeneratedValue(strategy=GenerationType.UUID) UUID id;
    UUID campaignId;
    String action;
    UUID actorId;
    Instant occurredAt;
    @Column(name = "rollback_reason", columnDefinition="text") String rollbackReason;
    @Column(columnDefinition="text") String detailJson;
}

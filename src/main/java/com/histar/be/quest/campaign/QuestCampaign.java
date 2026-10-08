package com.histar.be.quest.campaign;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quest_campaigns")
@Getter @Setter
public class QuestCampaign {
    @Id private UUID id;
    private UUID questId;
    private String campaignType;
    private String title;
    private Instant startsAt;
    private Instant endsAt;
    private boolean enabled;
}

package com.histar.be.quest.campaign;

import java.time.Instant;
import java.util.UUID;

public record QuestCampaignResponse(UUID id, UUID questId, String type, String title, Instant startsAt, Instant endsAt) {
    static QuestCampaignResponse from(QuestCampaign campaign) {
        return new QuestCampaignResponse(campaign.getId(), campaign.getQuestId(), campaign.getCampaignType(), campaign.getTitle(), campaign.getStartsAt(), campaign.getEndsAt());
    }
}

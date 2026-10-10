package com.histar.be.quest.campaign;

import java.time.Instant;
import java.util.UUID;

public record QuestCampaignResponse(UUID id, UUID questId, String type, String title, Instant startsAt, Instant endsAt,
                                    boolean enabled, PilotStatus pilotStatus, ReleaseDecision releaseDecision,
                                    String releaseNote, UUID releaseDecidedBy, Instant releaseDecidedAt) {
    static QuestCampaignResponse from(QuestCampaign campaign) {
        return new QuestCampaignResponse(campaign.getId(), campaign.getQuestId(), campaign.getCampaignType(), campaign.getTitle(),
                campaign.getStartsAt(), campaign.getEndsAt(), campaign.isEnabled(), campaign.getPilotStatus(),
                campaign.getReleaseDecision(), campaign.getReleaseNote(), campaign.getReleaseDecidedBy(), campaign.getReleaseDecidedAt());
    }
}

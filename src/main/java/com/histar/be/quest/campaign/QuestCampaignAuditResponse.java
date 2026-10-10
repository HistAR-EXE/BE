package com.histar.be.quest.campaign;

import java.time.Instant;
import java.util.UUID;

public record QuestCampaignAuditResponse(
        UUID id,
        UUID campaignId,
        String action,
        UUID actorId,
        Instant occurredAt,
        String rollbackReason,
        String detailJson) {
    static QuestCampaignAuditResponse from(QuestCampaignAudit audit) {
        return new QuestCampaignAuditResponse(
                audit.getId(), audit.getCampaignId(), audit.getAction(), audit.getActorId(), audit.getOccurredAt(),
                audit.getRollbackReason(), audit.getDetailJson());
    }
}

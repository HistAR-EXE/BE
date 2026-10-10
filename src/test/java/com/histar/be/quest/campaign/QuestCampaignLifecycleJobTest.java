package com.histar.be.quest.campaign;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class QuestCampaignLifecycleJobTest {
    @Test
    void expiry_job_unpublishes_server_side_expired_campaigns() {
        QuestCampaignRepository campaigns = Mockito.mock(QuestCampaignRepository.class);
        QuestCampaignAdminService admin = Mockito.mock(QuestCampaignAdminService.class);
        UUID campaignId = UUID.randomUUID();
        QuestCampaign campaign = QuestCampaign.builder().id(campaignId).enabled(true).build();
        when(campaigns.findActiveBefore(ArgumentMatchers.any(Instant.class))).thenReturn(List.of(campaign));

        new QuestCampaignLifecycleJob(campaigns, admin).expire();

        verify(admin).setPublished(ArgumentMatchers.any(UUID.class), ArgumentMatchers.eq(campaignId), ArgumentMatchers.eq(false));
    }
}

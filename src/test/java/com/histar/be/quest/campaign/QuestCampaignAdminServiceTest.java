package com.histar.be.quest.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.quest.narrative.NarrativeInvestigationRepository;
import com.histar.be.quest.narrative.entity.NarrativeInvestigation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestCampaignAdminServiceTest {
    @Mock private QuestCampaignRepository campaigns;
    @Mock private QuestCampaignAuditRepository audits;
    @Mock private NarrativeInvestigationRepository investigations;
    private QuestCampaignAdminService service;

    @BeforeEach
    void setUp() {
        service = new QuestCampaignAdminService(campaigns, audits, new ObjectMapper(), investigations);
    }

    @Test
    void daily_campaign_requires_explicit_passed_pilot_and_approved_release_decision() {
        UUID questId = UUID.randomUUID();
        QuestCampaign campaign = campaign(questId, "DAILY");
        campaign.setPilotStatus(PilotStatus.PENDING);
        campaign.setReleaseDecision(ReleaseDecision.PENDING);
        when(campaigns.findById(campaign.getId())).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> service.setPublished(UUID.randomUUID(), campaign.getId(), true))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pilot");
    }

    @Test
    void daily_campaign_publishes_only_after_pilot_gate_and_release_decision() {
        UUID questId = UUID.randomUUID();
        QuestCampaign campaign = campaign(questId, "DAILY");
        campaign.setPilotStatus(PilotStatus.PASSED);
        campaign.setReleaseDecision(ReleaseDecision.APPROVED);
        when(campaigns.findById(campaign.getId())).thenReturn(Optional.of(campaign));
        when(campaigns.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(investigations.findByQuestId(questId)).thenReturn(Optional.of(NarrativeInvestigation.builder()
                .questId(questId).reviewStatus("APPROVED").reviewedAt(Instant.now()).build()));

        QuestCampaignResponse response = service.setPublished(UUID.randomUUID(), campaign.getId(), true);

        assertThat(response.releaseDecision()).isEqualTo(ReleaseDecision.APPROVED);
        assertThat(campaign.isEnabled()).isTrue();
    }

    @Test
    void rollback_returns_structured_reason_in_audit_dto() {
        QuestCampaign campaign = campaign(UUID.randomUUID(), "EVENT");
        when(campaigns.existsById(campaign.getId())).thenReturn(true);
        when(audits.findByCampaignIdOrderByOccurredAtDesc(campaign.getId())).thenReturn(List.of(
                QuestCampaignAudit.builder().campaignId(campaign.getId()).action("ROLLED_BACK")
                        .rollbackReason("content mismatch").actorId(UUID.randomUUID()).occurredAt(Instant.now()).build()));

        List<QuestCampaignAuditResponse> history = service.history(campaign.getId());

        assertThat(history).singleElement().satisfies(audit -> {
            assertThat(audit.action()).isEqualTo("ROLLED_BACK");
            assertThat(audit.rollbackReason()).isEqualTo("content mismatch");
        });
    }

    private static QuestCampaign campaign(UUID questId, String type) {
        QuestCampaign campaign = new QuestCampaign();
        campaign.setId(UUID.randomUUID());
        campaign.setQuestId(questId);
        campaign.setCampaignType(type);
        campaign.setTitle("Pilot campaign");
        campaign.setStartsAt(Instant.now().minusSeconds(60));
        campaign.setEndsAt(Instant.now().plusSeconds(3600));
        return campaign;
    }
}

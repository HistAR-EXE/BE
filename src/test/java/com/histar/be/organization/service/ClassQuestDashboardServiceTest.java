package com.histar.be.organization.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.organization.entity.OrganizationMember;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.quest.campaign.QuestCampaign;
import com.histar.be.quest.campaign.QuestCampaignRepository;
import com.histar.be.quest.narrative.QuestRewardLedgerRepository;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassQuestDashboardServiceTest {
    @Mock private OrganizationMemberRepository members;
    @Mock private UserQuestProgressRepository progress;
    @Mock private QuestRewardLedgerRepository rewards;
    @Mock private QuestCampaignRepository campaigns;
    @Mock private OrgAccessService orgAccessService;
    private ClassQuestDashboardService service;

    @BeforeEach
    void setUp() {
        service = new ClassQuestDashboardService(members, progress, rewards, campaigns, orgAccessService);
    }

    @Test
    void teacher_cannot_read_dashboard_for_another_organization() {
        UUID actor = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        doThrow(new BusinessRuleException("Không có quyền truy cập organization này"))
                .when(orgAccessService).requireOrgAccess(actor, orgId);

        assertThatThrownBy(() -> service.summary(actor, orgId, campaignId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("organization");
    }

    @Test
    void campaign_scope_resolves_quest_without_exposing_raw_student_memory() {
        UUID orgId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        when(campaigns.findById(campaignId)).thenReturn(Optional.of(QuestCampaign.builder()
                .id(campaignId).questId(questId).campaignType("WEEKLY").build()));
        when(members.findByOrganizationId(orgId)).thenReturn(java.util.List.of(
                OrganizationMember.builder().organizationId(orgId).userId(UUID.randomUUID()).orgRole("student").build()));
        when(progress.findByUserIdAndQuestId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(questId)))
                .thenReturn(Optional.empty());
        when(rewards.findByUserIdAndQuestId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(questId)))
                .thenReturn(java.util.List.of());

        service.summary(UUID.randomUUID(), orgId, campaignId);
    }
}

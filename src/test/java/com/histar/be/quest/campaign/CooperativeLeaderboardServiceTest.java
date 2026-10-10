package com.histar.be.quest.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.histar.be.group.entity.StudyGroup;
import com.histar.be.group.entity.StudyGroupMember;
import com.histar.be.group.repository.StudyGroupMemberRepository;
import com.histar.be.group.repository.StudyGroupRepository;
import com.histar.be.quest.narrative.QuestRewardLedgerRepository;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CooperativeLeaderboardServiceTest {
    @Mock private StudyGroupRepository groups;
    @Mock private StudyGroupMemberRepository members;
    @Mock private UserQuestProgressRepository progress;
    @Mock private QuestRewardLedgerRepository rewards;
    @Mock private QuestCampaignRepository campaigns;

    @Test
    void scores_groups_by_campaign_scoped_completion_evidence_and_verified_outcomes() {
        UUID orgId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        StudyGroup group = StudyGroup.builder().id(UUID.randomUUID()).orgId(orgId).questId(questId).name("A").build();
        when(campaigns.findById(campaignId)).thenReturn(java.util.Optional.of(
                QuestCampaign.builder().id(campaignId).questId(questId).enabled(true)
                        .pilotStatus(PilotStatus.PASSED).releaseDecision(ReleaseDecision.APPROVED)
                        .startsAt(Instant.now().minusSeconds(60)).endsAt(Instant.now().plusSeconds(60)).build()));
        when(groups.findByOrgIdAndQuestId(orgId, questId)).thenReturn(List.of(group));
        when(members.findByGroupId(group.getId())).thenReturn(List.of(StudyGroupMember.builder().userId(userId).build()));
        when(progress.findByUserIdAndQuestId(userId, questId)).thenReturn(java.util.Optional.of(
                com.histar.be.quest.progress.entity.UserQuestProgress.builder().status("completed").build()));
        when(rewards.findByUserIdAndQuestId(userId, questId)).thenReturn(List.of(
                com.histar.be.quest.narrative.entity.QuestRewardLedgerEntry.builder()
                        .rewardType("NARRATIVE_EVIDENCE").rewardKey("evidence").build(),
                com.histar.be.quest.narrative.entity.QuestRewardLedgerEntry.builder()
                        .rewardType("NARRATIVE_OUTCOME").rewardKey("outcome-verified").build()));

        var result = new CooperativeLeaderboardService(groups, members, progress, rewards, campaigns).forCampaign(orgId, campaignId);

        assertThat(result).singleElement().satisfies(row -> {
            assertThat(row.completionRatePct()).isEqualTo(100.0);
            assertThat(row.evidenceCoveragePct()).isEqualTo(100.0);
            assertThat(row.verifiedOutcomePct()).isEqualTo(100.0);
            assertThat(row.cooperativeScore()).isEqualTo(100.0);
        });
    }
}

package com.histar.be.quest.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.histar.be.organization.entity.OrganizationMember;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignAudiencePreviewServiceTest {
    @Mock private QuestCampaignRepository campaigns;
    @Mock private OrganizationMemberRepository members;
    @Mock private ProfileRepository profiles;

    @Test
    void preview_uses_accepted_students_and_explains_exclusions() {
        UUID campaignId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID eligibleId = UUID.randomUUID();
        UUID unacceptedId = UUID.randomUUID();
        UUID mismatchId = UUID.randomUUID();
        when(campaigns.findById(campaignId)).thenReturn(Optional.of(QuestCampaign.builder()
                .id(campaignId).questId(questId).campaignType("WEEKLY").title("Pilot")
                .enabled(true).pilotStatus(PilotStatus.PASSED).releaseDecision(ReleaseDecision.APPROVED).build()));
        when(members.findByOrganizationId(orgId)).thenReturn(List.of(
                member(orgId, UUID.randomUUID(), "teacher", true),
                member(orgId, eligibleId, "student", true),
                member(orgId, unacceptedId, "student", false),
                member(orgId, mismatchId, "student", true)));
        when(profiles.findById(eligibleId)).thenReturn(Optional.of(Profile.builder().id(eligibleId).orgId(orgId).build()));
        when(profiles.findById(mismatchId)).thenReturn(Optional.of(Profile.builder().id(mismatchId).orgId(UUID.randomUUID()).build()));

        CampaignAudiencePreview preview = new CampaignAudiencePreviewService(campaigns, members, profiles)
                .preview(campaignId, orgId);

        assertThat(preview.questId()).isEqualTo(questId);
        assertThat(preview.eligibleStudents()).isEqualTo(1);
        assertThat(preview.excludedTeachersOrAdmins()).isEqualTo(1);
        assertThat(preview.excludedUnacceptedMembers()).isEqualTo(1);
        assertThat(preview.excludedProfileMismatches()).isEqualTo(1);
    }

    private static OrganizationMember member(UUID orgId, UUID userId, String role, boolean accepted) {
        return OrganizationMember.builder().organizationId(orgId).userId(userId).orgRole(role)
                .invitedAt(Instant.now()).acceptedAt(accepted ? Instant.now() : null).build();
    }
}

package com.histar.be.quest.campaign;

import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.organization.entity.OrganizationMember;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CampaignAudiencePreviewService {
    private final QuestCampaignRepository campaigns;
    private final OrganizationMemberRepository members;
    private final ProfileRepository profiles;

    @Transactional(readOnly = true)
    public CampaignAudiencePreview preview(UUID campaignId, UUID organizationId) {
        QuestCampaign campaign = campaigns.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest campaign not found"));
        List<OrganizationMember> rows = members.findByOrganizationId(organizationId);
        long teachers = rows.stream().filter(row -> !"student".equalsIgnoreCase(row.getOrgRole())).count();
        long unaccepted = rows.stream().filter(row -> row.getAcceptedAt() == null).count();
        long profileMismatches = rows.stream()
                .filter(row -> "student".equalsIgnoreCase(row.getOrgRole()) && row.getAcceptedAt() != null)
                .filter(row -> profiles.findById(row.getUserId()).map(profile -> !organizationId.equals(profile.getOrgId())).orElse(true))
                .count();
        long eligible = rows.stream()
                .filter(row -> "student".equalsIgnoreCase(row.getOrgRole()) && row.getAcceptedAt() != null)
                .filter(row -> profiles.findById(row.getUserId()).map(profile -> organizationId.equals(profile.getOrgId())).orElse(false))
                .count();
        return new CampaignAudiencePreview(campaignId, campaign.getQuestId(), campaign.getCampaignType(), campaign.getTitle(),
                campaign.isEnabled(), campaign.getPilotStatus(), campaign.getReleaseDecision(), organizationId,
                eligible, teachers, unaccepted, profileMismatches);
    }
}

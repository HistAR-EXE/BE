package com.histar.be.quest.campaign;

import java.util.UUID;

public record CampaignAudiencePreview(
        UUID campaignId,
        UUID questId,
        String campaignType,
        String campaignTitle,
        boolean campaignEnabled,
        PilotStatus pilotStatus,
        ReleaseDecision releaseDecision,
        UUID organizationId,
        long eligibleStudents,
        long excludedTeachersOrAdmins,
        long excludedUnacceptedMembers,
        long excludedProfileMismatches) {}

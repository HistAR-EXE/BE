package com.histar.be.organization.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.organization.dto.OrgRosterMemberResponse;
import com.histar.be.organization.dto.OrganizationAnalyticsResponse;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.entity.OrganizationMember;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationAnalyticsService {

    private static final UUID CU_CHI_HERITAGE_QUEST_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final ProfileRepository profileRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;

    public OrganizationAnalyticsResponse analytics(UUID orgId) {
        Organization org = organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new BusinessRuleException("Organization không tồn tại"));
        List<OrganizationMember> members = organizationMemberRepository.findByOrganizationId(orgId);
        List<UUID> memberIds = members.stream().map(OrganizationMember::getUserId).toList();
        long memberCount = memberIds.size();

        if (memberCount == 0) {
            return new OrganizationAnalyticsResponse(
                    org.getId(),
                    org.getName(),
                    inferType(org.getSlug()),
                    0L,
                    0.0,
                    0.0,
                    "Chưa có thành viên — seed organization_members để xem cohort.");
        }

        long withAnyCompleted = memberIds.stream()
                .filter(id -> userQuestProgressRepository.countByUserIdAndStatus(id, "completed") > 0)
                .count();
        long withHeritageCompleted = memberIds.stream()
                .filter(id -> userQuestProgressRepository
                        .findByUserIdAndQuestId(id, CU_CHI_HERITAGE_QUEST_ID)
                        .filter(p -> "completed".equalsIgnoreCase(p.getStatus()))
                        .isPresent())
                .count();

        double completionRatePct = roundPct(withAnyCompleted, memberCount);
        double fullTourRatePct = roundPct(withHeritageCompleted, memberCount);

        return new OrganizationAnalyticsResponse(
                org.getId(),
                org.getName(),
                inferType(org.getSlug()),
                memberCount,
                completionRatePct,
                fullTourRatePct,
                "Cohort thật từ organization_members + user_quest_progress.");
    }

    public List<OrgRosterMemberResponse> roster(UUID orgId) {
        organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new BusinessRuleException("Organization không tồn tại"));
        return organizationMemberRepository.findByOrganizationId(orgId).stream()
                .map(this::toRosterMember)
                .toList();
    }

    private OrgRosterMemberResponse toRosterMember(OrganizationMember member) {
        Profile profile = profileRepository.findById(member.getUserId()).orElse(null);
        String displayName = profile != null ? profile.getDisplayName() : "Unknown";
        String email = profile != null ? profile.getEmail() : "";
        int level = profile != null && profile.getLevel() != null ? profile.getLevel() : 1;
        int points = profile != null && profile.getTotalPoints() != null ? profile.getTotalPoints() : 0;
        long completed = userQuestProgressRepository.countByUserIdAndStatus(member.getUserId(), "completed");
        return new OrgRosterMemberResponse(
                member.getUserId(),
                displayName,
                email,
                member.getOrgRole(),
                level,
                points,
                completed);
    }

    private static double roundPct(long numerator, long denominator) {
        if (denominator <= 0) return 0.0;
        return Math.round((numerator * 10000.0) / denominator) / 100.0;
    }

    private static String inferType(String slug) {
        if (slug == null) return "museum";
        if (slug.contains("school") || slug.contains("fpt") || slug.contains("uni")) return "school";
        if (slug.contains("tour")) return "tour_company";
        return "museum";
    }
}

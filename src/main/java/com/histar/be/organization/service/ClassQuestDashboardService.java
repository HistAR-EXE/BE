package com.histar.be.organization.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.organization.dto.ClassQuestDashboardResponse;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.quest.campaign.QuestCampaign;
import com.histar.be.quest.campaign.QuestCampaignRepository;
import com.histar.be.quest.narrative.QuestRewardLedgerRepository;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClassQuestDashboardService {
    private final OrganizationMemberRepository members;
    private final UserQuestProgressRepository progress;
    private final QuestRewardLedgerRepository rewards;
    private final QuestCampaignRepository campaigns;
    private final OrgAccessService orgAccessService;

    @Transactional(readOnly = true)
    public ClassQuestDashboardResponse summary(UUID actorId, UUID orgId, UUID campaignId) {
        if (!orgAccessService.isPlatformAdmin() && !orgAccessService.isOrgTeacher(actorId, orgId)) {
            throw new BusinessRuleException("Không có quyền truy cập organization này");
        }
        QuestCampaign campaign = campaigns.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest campaign not found"));
        UUID questId = campaign.getQuestId();
        List<UUID> ids = members.findByOrganizationId(orgId).stream()
                .filter(member -> "student".equalsIgnoreCase(member.getOrgRole()))
                .map(member -> member.getUserId())
                .toList();
        long count = ids.size();
        long completed = ids.stream().filter(id -> progress.findByUserIdAndQuestId(id, questId)
                .map(p -> "completed".equalsIgnoreCase(p.getStatus())).orElse(false)).count();
        long evidence = ids.stream().filter(id -> rewards.findByUserIdAndQuestId(id, questId).stream()
                .anyMatch(reward -> "NARRATIVE_EVIDENCE".equals(reward.getRewardType()))).count();
        long verified = ids.stream().filter(id -> rewards.findByUserIdAndQuestId(id, questId).stream()
                .anyMatch(reward -> "NARRATIVE_OUTCOME".equals(reward.getRewardType())
                        && "outcome-verified".equals(reward.getRewardKey()))).count();
        String blocker = evidence < count ? "Thiếu chứng cứ đã xác minh"
                : completed < count ? "Chưa hoàn thành hành trình" : "Không có blocker nổi bật";
        return new ClassQuestDashboardResponse(orgId, campaignId, questId, count, completed, pct(completed, count),
                evidence, pct(evidence, count), verified, blocker);
    }

    private static double pct(long value, long total) {
        return total == 0 ? 0 : Math.round(value * 10000.0 / total) / 100.0;
    }
}

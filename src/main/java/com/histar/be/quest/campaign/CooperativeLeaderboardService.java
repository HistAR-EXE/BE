package com.histar.be.quest.campaign;
import com.histar.be.group.entity.StudyGroup;
import com.histar.be.group.repository.*;
import com.histar.be.quest.narrative.QuestRewardLedgerRepository;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

/** Group-only pilot ranking; never reads or ranks personal XP. */
@Service @RequiredArgsConstructor
public class CooperativeLeaderboardService {
 private final StudyGroupRepository groups; private final StudyGroupMemberRepository members;
 private final UserQuestProgressRepository progress; private final QuestRewardLedgerRepository rewards;
 private final QuestCampaignRepository campaigns;
 @Transactional(readOnly=true) public List<CooperativeLeaderboardEntry> forCampaign(UUID orgId, UUID campaignId) {
  QuestCampaign campaign = campaigns.findById(campaignId).orElseThrow(() -> new com.histar.be.common.exception.ResourceNotFoundException("Quest campaign not found"));
  if (!campaign.isEnabled() || campaign.getPilotStatus() != PilotStatus.PASSED || campaign.getReleaseDecision() != ReleaseDecision.APPROVED
      || Instant.now().isBefore(campaign.getStartsAt()) || !Instant.now().isBefore(campaign.getEndsAt())) throw new com.histar.be.common.exception.BusinessRuleException("Campaign không khả dụng");
  UUID questId = campaign.getQuestId();
  List<CooperativeLeaderboardEntry> rows=new ArrayList<>();
  for (StudyGroup group: groups.findByOrgIdAndQuestId(orgId,questId)) {
   List<UUID> ids=members.findByGroupId(group.getId()).stream().map(m->m.getUserId()).toList(); if(ids.isEmpty()) continue;
   long complete=ids.stream().filter(id->progress.findByUserIdAndQuestId(id,questId).map(p->"completed".equalsIgnoreCase(p.getStatus())).orElse(false)).count();
   long evidence=ids.stream().filter(id->rewards.findByUserIdAndQuestId(id,questId).stream().anyMatch(r->"NARRATIVE_EVIDENCE".equals(r.getRewardType()))).count();
   long verified=ids.stream().filter(id->rewards.findByUserIdAndQuestId(id,questId).stream().anyMatch(r->"NARRATIVE_OUTCOME".equals(r.getRewardType()) && "outcome-verified".equals(r.getRewardKey()))).count();
   double completion=pct(complete,ids.size()), coverage=pct(evidence,ids.size()), outcome=pct(verified,ids.size());
   rows.add(new CooperativeLeaderboardEntry(0,group.getId(),group.getName(),ids.size(),completion,coverage,outcome,round(completion*.60+coverage*.25+outcome*.15)));
  }
  rows.sort(Comparator.comparingDouble(CooperativeLeaderboardEntry::cooperativeScore).reversed().thenComparing(CooperativeLeaderboardEntry::groupName));
  List<CooperativeLeaderboardEntry> ranked=new ArrayList<>(); for(int i=0;i<rows.size();i++){var r=rows.get(i);ranked.add(new CooperativeLeaderboardEntry(i+1,r.groupId(),r.groupName(),r.members(),r.completionRatePct(),r.evidenceCoveragePct(),r.verifiedOutcomePct(),r.cooperativeScore()));} return ranked;
 }
 private static double pct(long n,long d){return d==0?0:round(n*100.0/d);} private static double round(double n){return Math.round(n*100.0)/100.0;}
}

package com.histar.be.quest.narrative;
import com.histar.be.profile.service.ProfilePointsService;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.narrative.entity.QuestRewardLedgerEntry;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor public class NarrativeRewardService {
 private final QuestRewardLedgerRepository ledger; private final ProfilePointsService points; private final ProfileRepository profiles;
 @Transactional public int awardOnce(UUID user, UUID quest, String type, String key, int xp) {
  // Serialising reward decisions on the profile row makes the unique ledger constraint a
  // backstop rather than the normal concurrency-control path.
  profiles.findByIdForUpdate(user).orElseThrow();
  if (ledger.existsByUserIdAndQuestIdAndRewardTypeAndRewardKey(user,quest,type,key)) return 0;
  ledger.save(QuestRewardLedgerEntry.builder().userId(user).questId(quest).rewardType(type).rewardKey(key).xp(xp).awardedAt(Instant.now()).build());
  return points.award(user,xp);
 }
}

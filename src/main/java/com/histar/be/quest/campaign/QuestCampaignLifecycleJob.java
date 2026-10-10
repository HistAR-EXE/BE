package com.histar.be.quest.campaign;
import java.time.Instant; import java.util.UUID; import lombok.RequiredArgsConstructor; import net.javacrumbs.shedlock.spring.annotation.SchedulerLock; import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component;
/** Server owns expiry; browser clocks never activate or retain a campaign. */
@Component @RequiredArgsConstructor public class QuestCampaignLifecycleJob {
 private static final UUID SYSTEM=UUID.fromString("00000000-0000-0000-0000-000000000001"); private final QuestCampaignRepository campaigns; private final QuestCampaignAdminService admin;
 @Scheduled(cron="0 */5 * * * *") @SchedulerLock(name="questCampaignExpiry",lockAtMostFor="PT4M",lockAtLeastFor="PT30S")
 public void expire(){ for(QuestCampaign c:campaigns.findActiveBefore(Instant.now())) admin.setPublished(SYSTEM,c.getId(),false); }
}

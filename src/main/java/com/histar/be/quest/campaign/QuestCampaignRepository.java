package com.histar.be.quest.campaign;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuestCampaignRepository extends JpaRepository<QuestCampaign, UUID> {
    @Query("SELECT c FROM QuestCampaign c WHERE c.enabled = true AND c.startsAt <= :now AND c.endsAt > :now ORDER BY c.endsAt")
    List<QuestCampaign> findActive(Instant now);
}

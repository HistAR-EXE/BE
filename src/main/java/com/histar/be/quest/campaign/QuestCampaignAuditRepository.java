package com.histar.be.quest.campaign;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface QuestCampaignAuditRepository extends JpaRepository<QuestCampaignAudit, UUID> { List<QuestCampaignAudit> findByCampaignIdOrderByOccurredAtDesc(UUID campaignId); }

package com.histar.be.campaign.repository;

import com.histar.be.campaign.entity.Campaign;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
}

package com.histar.be.campaign.service;

import com.histar.be.campaign.entity.Campaign;
import java.util.List;
import java.util.UUID;


public interface CampaignService {

    List<Campaign> findAll();

    Campaign findById(UUID id);

    Campaign save(Campaign entity);

    void deleteById(UUID id);

    long count();
}

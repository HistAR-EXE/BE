package com.histar.be.campaign.service.impl;

import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.campaign.entity.Campaign;
import com.histar.be.campaign.repository.CampaignRepository;
import com.histar.be.campaign.service.CampaignService;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {

    private final CampaignRepository repository;

    @Override
    public List<Campaign> findAll() {
        return repository.findAll();
    }

    @Override
    public Campaign findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + id));
    }

    @Override
    public Campaign save(Campaign entity) {
        return repository.save(entity);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public long count() {
        return repository.count();
    }
}

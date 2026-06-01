package com.histar.be.badge.service.impl;

import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.badge.entity.Badge;
import com.histar.be.badge.repository.BadgeRepository;
import com.histar.be.badge.service.BadgeService;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BadgeServiceImpl implements BadgeService {

    private final BadgeRepository repository;

    @Override
    public List<Badge> findAll() {
        return repository.findAll();
    }

    @Override
    public Badge findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Badge not found: " + id));
    }

    @Override
    public Badge save(Badge entity) {
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

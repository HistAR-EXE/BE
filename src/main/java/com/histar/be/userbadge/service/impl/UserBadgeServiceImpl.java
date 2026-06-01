package com.histar.be.userbadge.service.impl;

import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.userbadge.entity.UserBadge;
import com.histar.be.userbadge.repository.UserBadgeRepository;
import com.histar.be.userbadge.service.UserBadgeService;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserBadgeServiceImpl implements UserBadgeService {

    private final UserBadgeRepository repository;

    @Override
    public List<UserBadge> findAll() {
        return repository.findAll();
    }

    @Override
    public UserBadge findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserBadge not found: " + id));
    }

    @Override
    public UserBadge save(UserBadge entity) {
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

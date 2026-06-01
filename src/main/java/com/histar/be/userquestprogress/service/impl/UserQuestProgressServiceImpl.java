package com.histar.be.userquestprogress.service.impl;

import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.userquestprogress.entity.UserQuestProgress;
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
import com.histar.be.userquestprogress.service.UserQuestProgressService;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserQuestProgressServiceImpl implements UserQuestProgressService {

    private final UserQuestProgressRepository repository;

    @Override
    public List<UserQuestProgress> findAll() {
        return repository.findAll();
    }

    @Override
    public UserQuestProgress findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserQuestProgress not found: " + id));
    }

    @Override
    public UserQuestProgress save(UserQuestProgress entity) {
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

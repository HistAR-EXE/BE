package com.histar.be.usercreation.service.impl;

import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.usercreation.entity.UserCreation;
import com.histar.be.usercreation.repository.UserCreationRepository;
import com.histar.be.usercreation.service.UserCreationService;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCreationServiceImpl implements UserCreationService {

    private final UserCreationRepository repository;

    @Override
    public List<UserCreation> findAll() {
        return repository.findAll();
    }

    @Override
    public UserCreation findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserCreation not found: " + id));
    }

    @Override
    public UserCreation save(UserCreation entity) {
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

package com.histar.be.profile.service.impl;

import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.profile.service.ProfileService;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository repository;

    @Override
    public List<Profile> findAll() {
        return repository.findAll();
    }

    @Override
    public Profile findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found: " + id));
    }

    @Override
    public Profile save(Profile entity) {
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
    @Override
    public Optional<Profile> findByEmail(String email) {
        return repository.findByEmail(email);
    }

}

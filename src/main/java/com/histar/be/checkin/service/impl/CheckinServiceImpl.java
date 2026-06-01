package com.histar.be.checkin.service.impl;

import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.checkin.entity.Checkin;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.checkin.service.CheckinService;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckinServiceImpl implements CheckinService {

    private final CheckinRepository repository;

    @Override
    public List<Checkin> findAll() {
        return repository.findAll();
    }

    @Override
    public Checkin findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found: " + id));
    }

    @Override
    public Checkin save(Checkin entity) {
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

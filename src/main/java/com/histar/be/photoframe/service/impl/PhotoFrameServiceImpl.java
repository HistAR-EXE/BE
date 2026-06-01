package com.histar.be.photoframe.service.impl;

import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.photoframe.entity.PhotoFrame;
import com.histar.be.photoframe.repository.PhotoFrameRepository;
import com.histar.be.photoframe.service.PhotoFrameService;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PhotoFrameServiceImpl implements PhotoFrameService {

    private final PhotoFrameRepository repository;

    @Override
    public List<PhotoFrame> findAll() {
        return repository.findAll();
    }

    @Override
    public PhotoFrame findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PhotoFrame not found: " + id));
    }

    @Override
    public PhotoFrame save(PhotoFrame entity) {
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

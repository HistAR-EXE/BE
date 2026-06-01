package com.histar.be.photopair.service.impl;

import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.photopair.entity.PhotoPair;
import com.histar.be.photopair.repository.PhotoPairRepository;
import com.histar.be.photopair.service.PhotoPairService;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PhotoPairServiceImpl implements PhotoPairService {

    private final PhotoPairRepository repository;

    @Override
    public List<PhotoPair> findAll() {
        return repository.findAll();
    }

    @Override
    public PhotoPair findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PhotoPair not found: " + id));
    }

    @Override
    public PhotoPair save(PhotoPair entity) {
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
    public List<PhotoPair> findByLocationIdOrderBySortOrder(UUID locationId) {
        return repository.findByLocationIdOrderBySortOrder(locationId);
    }

}

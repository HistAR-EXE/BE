package com.histar.be.panorama.service.impl;

import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.panorama.entity.Panorama;
import com.histar.be.panorama.repository.PanoramaRepository;
import com.histar.be.panorama.service.PanoramaService;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PanoramaServiceImpl implements PanoramaService {

    private final PanoramaRepository repository;

    @Override
    public List<Panorama> findAll() {
        return repository.findAll();
    }

    @Override
    public Panorama findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Panorama not found: " + id));
    }

    @Override
    public Panorama save(Panorama entity) {
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
    public List<Panorama> findByLocationId(UUID locationId) {
        return repository.findByLocationId(locationId);
    }
}

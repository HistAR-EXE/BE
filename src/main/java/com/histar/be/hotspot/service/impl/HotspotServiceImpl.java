package com.histar.be.hotspot.service.impl;

import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.hotspot.dto.HotspotResponse;
import com.histar.be.hotspot.entity.Hotspot;
import com.histar.be.hotspot.repository.HotspotRepository;
import com.histar.be.hotspot.service.HotspotService;
import com.histar.be.hotspot.content.repository.HotspotContentRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HotspotServiceImpl implements HotspotService {

    private final HotspotRepository repository;
    private final HotspotContentRepository hotspotContentRepository;

    @Override
    public List<Hotspot> findAll() {
        return repository.findAll();
    }

    @Override
    public Hotspot findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotspot not found: " + id));
    }

    @Override
    public Hotspot save(Hotspot entity) {
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
    public List<Hotspot> findByPanoramaId(UUID panoramaId) {
        return repository.findByPanoramaId(panoramaId);
    }

    @Override
    public List<HotspotResponse> findResponsesByPanoramaId(UUID panoramaId) {
        return repository.findByPanoramaId(panoramaId).stream()
                .map(this::toResponse)
                .toList();
    }

    private HotspotResponse toResponse(Hotspot hotspot) {
        if (hotspot.getContentRef() == null) {
            return HotspotResponse.from(hotspot);
        }
        return hotspotContentRepository
                .findById(hotspot.getContentRef())
                .map(content -> HotspotResponse.from(
                        hotspot,
                        content.getTitle(),
                        content.getDescription(),
                        content.getImageUrl(),
                        content.getUnlockKey()))
                .orElseGet(() -> HotspotResponse.from(hotspot));
    }
}

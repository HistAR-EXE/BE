package com.histar.be.photoscene.service.impl;

import com.histar.be.photoscene.dto.PhotoSceneResponse;
import com.histar.be.photoscene.dto.TimeLayerResponse;
import com.histar.be.photoscene.repository.PhotoSceneRepository;
import com.histar.be.photoscene.repository.TimeLayerRepository;
import com.histar.be.photoscene.service.PhotoSceneService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PhotoSceneServiceImpl implements PhotoSceneService {

    private final PhotoSceneRepository photoSceneRepository;
    private final TimeLayerRepository timeLayerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PhotoSceneResponse> findByLocationId(UUID locationId) {
        return photoSceneRepository.findByLocationIdOrderBySortOrder(locationId).stream()
                .map(scene -> {
                    List<TimeLayerResponse> layers = timeLayerRepository.findBySceneIdOrderByEra(scene.getId()).stream()
                            .map(TimeLayerResponse::from)
                            .toList();
                    return PhotoSceneResponse.from(scene, layers);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return photoSceneRepository.count();
    }
}

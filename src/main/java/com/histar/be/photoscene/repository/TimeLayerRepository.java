package com.histar.be.photoscene.repository;

import com.histar.be.photoscene.entity.TimeLayer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeLayerRepository extends JpaRepository<TimeLayer, UUID> {

    List<TimeLayer> findBySceneIdOrderByEra(UUID sceneId);
}

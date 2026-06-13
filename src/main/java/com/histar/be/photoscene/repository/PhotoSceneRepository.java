package com.histar.be.photoscene.repository;

import com.histar.be.photoscene.entity.PhotoScene;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoSceneRepository extends JpaRepository<PhotoScene, UUID> {

    List<PhotoScene> findByLocationIdOrderBySortOrder(UUID locationId);
}

package com.histar.be.panorama.repository;

import com.histar.be.panorama.entity.Panorama;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PanoramaRepository extends JpaRepository<Panorama, UUID> {

    List<Panorama> findByLocationId(UUID locationId);
}

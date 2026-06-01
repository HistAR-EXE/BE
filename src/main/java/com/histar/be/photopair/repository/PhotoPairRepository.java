package com.histar.be.photopair.repository;

import com.histar.be.photopair.entity.PhotoPair;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoPairRepository extends JpaRepository<PhotoPair, UUID> {

    List<PhotoPair> findByLocationIdOrderBySortOrder(UUID locationId);
}

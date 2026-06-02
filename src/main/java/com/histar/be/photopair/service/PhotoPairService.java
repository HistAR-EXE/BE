package com.histar.be.photopair.service;

import com.histar.be.photopair.entity.PhotoPair;
import java.util.List;
import java.util.UUID;

public interface PhotoPairService {

    List<PhotoPair> findAll();

    PhotoPair findById(UUID id);

    PhotoPair save(PhotoPair entity);

    void deleteById(UUID id);

    long count();
    List<PhotoPair> findByLocationIdOrderBySortOrder(UUID locationId);
}

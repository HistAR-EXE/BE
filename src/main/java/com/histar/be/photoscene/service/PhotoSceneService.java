package com.histar.be.photoscene.service;

import com.histar.be.photoscene.dto.PhotoSceneResponse;
import java.util.List;
import java.util.UUID;

public interface PhotoSceneService {

    List<PhotoSceneResponse> findByLocationId(UUID locationId);

    long count();
}

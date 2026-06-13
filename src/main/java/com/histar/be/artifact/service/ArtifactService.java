package com.histar.be.artifact.service;

import com.histar.be.artifact.dto.ArtifactResponse;
import com.histar.be.artifact.dto.MyArtifactsResponse;
import java.util.List;
import java.util.UUID;

public interface ArtifactService {

    List<ArtifactResponse> findCatalog(UUID locationId);

    MyArtifactsResponse findMine(UUID userId, UUID locationId);

    boolean unlockByKey(UUID userId, String unlockKey);

    void unlockOnCheckin(UUID userId, UUID locationId);

    long count();
}

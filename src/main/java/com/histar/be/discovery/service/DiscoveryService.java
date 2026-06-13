package com.histar.be.discovery.service;

import com.histar.be.discovery.dto.DiscoveryPointResponse;
import com.histar.be.discovery.dto.DiscoverySummaryResponse;
import java.util.List;
import java.util.UUID;

public interface DiscoveryService {

    List<DiscoveryPointResponse> findPointsByLocation(UUID locationId);

    DiscoverySummaryResponse summary(UUID userId, UUID locationId);

    boolean record(UUID userId, String unlockKey);

    boolean record(UUID userId, String unlockKey, UUID locationId);

    void recordOnCheckin(UUID userId, UUID locationId);

    long countPoints();
}

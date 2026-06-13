package com.histar.be.discovery.dto;

import com.histar.be.discovery.entity.DiscoveryPoint;
import java.math.BigDecimal;
import java.util.UUID;

public record DiscoveryPointResponse(
        UUID id, String name, BigDecimal mapXPct, BigDecimal mapYPct, String unlockKey, Integer sortOrder) {

    public static DiscoveryPointResponse from(DiscoveryPoint point) {
        return new DiscoveryPointResponse(
                point.getId(),
                point.getName(),
                point.getMapXPct(),
                point.getMapYPct(),
                point.getUnlockKey(),
                point.getSortOrder());
    }
}

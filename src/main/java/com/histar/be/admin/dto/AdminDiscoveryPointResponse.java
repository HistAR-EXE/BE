package com.histar.be.admin.dto;

import com.histar.be.discovery.entity.DiscoveryPoint;
import java.math.BigDecimal;
import java.util.UUID;

public record AdminDiscoveryPointResponse(
        UUID id,
        UUID locationId,
        String name,
        BigDecimal mapXPct,
        BigDecimal mapYPct,
        String unlockKey,
        Integer sortOrder) {

    public static AdminDiscoveryPointResponse from(DiscoveryPoint point) {
        return new AdminDiscoveryPointResponse(
                point.getId(),
                point.getLocationId(),
                point.getName(),
                point.getMapXPct(),
                point.getMapYPct(),
                point.getUnlockKey(),
                point.getSortOrder());
    }
}

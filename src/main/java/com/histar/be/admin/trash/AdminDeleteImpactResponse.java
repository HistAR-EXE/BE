package com.histar.be.admin.trash;

import java.util.List;
import java.util.UUID;

public record AdminDeleteImpactResponse(
        String resourceType,
        UUID resourceId,
        boolean deletable,
        List<ImpactItem> blockers,
        List<ImpactItem> warnings) {

    public record ImpactItem(String code, long count, String message) {}
}

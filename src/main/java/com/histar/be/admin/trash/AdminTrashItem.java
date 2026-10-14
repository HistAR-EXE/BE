package com.histar.be.admin.trash;

import java.time.Instant;
import java.util.UUID;

public record AdminTrashItem(
        String resourceType,
        UUID id,
        UUID locationId,
        String name,
        Instant deletedAt,
        UUID deletedBy,
        String deleteReason,
        Long rowVersion) {}

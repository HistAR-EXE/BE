package com.histar.be.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record OrgArCardResponse(
        UUID id,
        UUID organizationId,
        UUID userId,
        String email,
        String displayName,
        String cardCode,
        String status,
        Instant issuedAt,
        Instant revokedAt) {}

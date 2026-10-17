package com.histar.be.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record OrgCertificateResponse(
        UUID id,
        UUID organizationId,
        UUID userId,
        String email,
        String displayName,
        String title,
        String certificateCode,
        Instant issuedAt) {}

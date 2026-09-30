package com.histar.be.admin.dto;

import java.util.UUID;

public record AdminOrganizationSummaryResponse(
        UUID id,
        String name,
        String orgType,
        String planType,
        String plan,
        long memberCount) {}

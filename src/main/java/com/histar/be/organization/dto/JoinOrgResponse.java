package com.histar.be.organization.dto;

import java.util.UUID;

public record JoinOrgResponse(
        UUID organizationId, String organizationName, String orgRole, String platformRole) {}

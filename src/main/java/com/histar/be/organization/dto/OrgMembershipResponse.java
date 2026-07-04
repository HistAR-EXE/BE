package com.histar.be.organization.dto;

import java.util.UUID;

public record OrgMembershipResponse(UUID organizationId, String name, String orgRole) {}

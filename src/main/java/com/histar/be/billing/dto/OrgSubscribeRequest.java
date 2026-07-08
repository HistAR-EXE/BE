package com.histar.be.billing.dto;

import java.util.UUID;

public record OrgSubscribeRequest(
        String orgName, String planType, String contactEmail, UUID organizationId) {}

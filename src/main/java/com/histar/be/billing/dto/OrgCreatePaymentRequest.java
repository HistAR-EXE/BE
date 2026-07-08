package com.histar.be.billing.dto;

import java.util.UUID;

public record OrgCreatePaymentRequest(
        String orgName,
        String planType,
        String contactEmail,
        UUID organizationId,
        String returnToPath,
        Integer licenseCount) {}

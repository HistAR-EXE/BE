package com.histar.be.billing.dto;

import java.time.LocalDate;
import java.util.UUID;

public record OrgBillingStatus(
        UUID organizationId,
        String orgName,
        String planType,
        LocalDate endDate,
        boolean isActive,
        int aiQueriesUsed,
        Integer aiQueriesLimit,
        LocalDate quotaResetsOn,
        int verifiedAccounts,
        int maxVerifiedAccounts,
        boolean accountLimitReached,
        int ccuCurrent,
        int maxCcu,
        boolean ccuLimitReached,
        int daysUntilExpiry) {}

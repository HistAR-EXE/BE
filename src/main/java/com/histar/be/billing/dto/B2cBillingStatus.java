package com.histar.be.billing.dto;

import java.time.LocalDate;

public record B2cBillingStatus(String tier, LocalDate endDate, boolean isActive, int priceVnd, int daysUntilExpiry) {}

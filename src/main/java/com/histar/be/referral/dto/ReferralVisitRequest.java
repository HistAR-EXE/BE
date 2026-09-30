package com.histar.be.referral.dto;

import jakarta.validation.constraints.Size;

public record ReferralVisitRequest(@Size(max = 64) String sessionId) {}

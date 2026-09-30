package com.histar.be.referral.dto;

/** Public, non-PII creator dashboard stats (C4). */
public record CreatorPublicStats(String code, String creatorName, long visits30d, long uniqueUsers30d) {}

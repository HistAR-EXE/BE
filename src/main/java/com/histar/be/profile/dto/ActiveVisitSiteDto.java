package com.histar.be.profile.dto;

import java.time.Instant;

/** Active Journey Pass (or visit entitlement) for a pilot site. */
public record ActiveVisitSiteDto(String siteCode, Instant expiresAt) {}

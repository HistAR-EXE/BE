package com.histar.be.profile.dto;

import java.time.Instant;
import java.util.UUID;

public record PassportStampResponse(
        UUID locationId, String locationName, Instant completedAt, boolean arStampEligible) {}

package com.histar.be.pilot.dto;

import java.util.UUID;

public record PilotSiteResponse(
        String siteCode,
        UUID locationId,
        String name,
        String region,
        String formattedAddress,
        Double latitude,
        Double longitude,
        boolean onsiteReady) {}

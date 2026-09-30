package com.histar.be.checkin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Legacy fields ({@code qrCode}, GPS) remain supported. A check-in needs either a location
 * {@code qrCode} or a signed station {@code qrPayload}; GPS is optional when a verified station QR is sent.
 */
public record CheckinRequest(
        @NotNull UUID locationId,
        Double latitude,
        Double longitude,
        String qrCode,
        @Size(max = 64) String stationCode,
        @Size(max = 512) String qrPayload,
        @Size(max = 16) String presenceMethod,
        UUID clientUuid) {}

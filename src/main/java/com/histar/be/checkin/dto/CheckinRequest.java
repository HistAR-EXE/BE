package com.histar.be.checkin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CheckinRequest(
        @NotNull UUID locationId,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotBlank String qrCode) {}

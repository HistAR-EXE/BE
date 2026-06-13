package com.histar.be.discovery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RecordDiscoveryRequest(
        @NotBlank String unlockKey, String source, @NotNull UUID locationId) {}

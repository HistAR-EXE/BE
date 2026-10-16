package com.histar.be.discovery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RecordDiscoveryRequest(
        @NotBlank @jakarta.validation.constraints.Size(max = 128) String unlockKey,
        @jakarta.validation.constraints.Size(max = 32) String source, @NotNull UUID locationId) {}

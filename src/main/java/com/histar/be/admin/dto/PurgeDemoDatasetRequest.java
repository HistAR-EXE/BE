package com.histar.be.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PurgeDemoDatasetRequest(
        UUID userId,
        @NotBlank @Size(min = 10, max = 500) String reason) {}

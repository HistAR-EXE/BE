package com.histar.be.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserTierRequest(
        @NotBlank @Pattern(regexp = "FREE|PREMIUM", message = "tier must be FREE or PREMIUM") String tier,
        @NotBlank @Size(min = 10, max = 1000) String reason,
        @NotBlank @Pattern(regexp = "FREE|PREMIUM", message = "expectedTier must be FREE or PREMIUM") String expectedTier) {}

package com.histar.be.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserStatusRequest(
        @NotBlank @Pattern(regexp = "ACTIVE|SUSPENDED", message = "status must be ACTIVE or SUSPENDED") String status,
        @NotBlank @Size(min = 10, max = 1000) String reason,
        @NotBlank
                @Pattern(regexp = "ACTIVE|SUSPENDED", message = "expectedStatus must be ACTIVE or SUSPENDED")
                String expectedStatus) {}

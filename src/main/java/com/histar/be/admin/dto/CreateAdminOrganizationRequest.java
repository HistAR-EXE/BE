package com.histar.be.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAdminOrganizationRequest(
        @NotBlank @Size(min = 2, max = 200) String name,
        @Size(max = 64) String planType,
        @Email @Size(max = 255) String contactEmail,
        @Size(max = 64) String status) {}

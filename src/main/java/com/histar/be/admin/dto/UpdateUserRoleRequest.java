package com.histar.be.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRoleRequest(
        @NotBlank @Pattern(regexp = "USER|ADMIN", message = "role must be USER or ADMIN") String role) {}

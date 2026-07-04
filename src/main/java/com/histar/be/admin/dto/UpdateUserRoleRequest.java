package com.histar.be.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRoleRequest(
        @NotBlank @Pattern(regexp = "USER|ADMIN|TEACHER", message = "role must be USER, ADMIN, or TEACHER") String role) {}

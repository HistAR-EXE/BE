package com.histar.be.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRoleRequest(
        @NotBlank @Pattern(regexp = "USER|ORG_MEMBER|ADMIN|TEACHER", message = "role must be USER, ORG_MEMBER, ADMIN, or TEACHER") String role,
        @NotBlank @Size(min = 10, max = 1000) String reason,
        @NotBlank @Pattern(regexp = "USER|ORG_MEMBER|ADMIN|TEACHER", message = "expectedRole must be USER, ORG_MEMBER, ADMIN, or TEACHER") String expectedRole) {}

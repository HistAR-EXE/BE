package com.histar.be.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRoleRequest(
        @NotBlank @Pattern(regexp = "USER|ORG_MEMBER|ADMIN|TEACHER", message = "role must be USER, ORG_MEMBER, ADMIN, or TEACHER") String role) {}

package com.histar.be.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record AdminUserBulkRequest(
        @NotEmpty @Size(max = 100) List<UUID> userIds,
        @NotBlank
                @Pattern(
                        regexp = "SUSPEND|UNSUSPEND|SET_ROLE|SET_TIER",
                        message = "action must be SUSPEND, UNSUSPEND, SET_ROLE, or SET_TIER")
                String action,
        @Size(max = 1000) String reason,
        @Pattern(regexp = "USER|ORG_MEMBER|ADMIN|TEACHER") String role,
        @Pattern(regexp = "FREE|PREMIUM") String tier,
        @Pattern(regexp = "ACTIVE|SUSPENDED") String status) {}

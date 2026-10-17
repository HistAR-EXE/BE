package com.histar.be.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for PATCH /api/admin/locations/{id}/ar-availability.
 * Reason is required so the audit log is always meaningful.
 */
public record LocationArToggleRequest(
        @NotNull boolean isArAvailable,
        @NotNull @Size(min = 3, max = 500) String reason) {}

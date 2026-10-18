package com.histar.be.admin.trash;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminPurgeRequest(
        @NotNull Long rowVersion,
        @NotBlank @Size(min = 10, max = 2000) String reason,
        String idempotencyKey) {}

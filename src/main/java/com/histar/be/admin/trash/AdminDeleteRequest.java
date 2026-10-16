package com.histar.be.admin.trash;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminDeleteRequest(
        @NotBlank String reasonPreset,
        String reasonDetail,
        @NotNull Long rowVersion,
        String idempotencyKey) {}

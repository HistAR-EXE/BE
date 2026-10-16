package com.histar.be.admin.trash;

import jakarta.validation.constraints.NotNull;

public record AdminRestoreRequest(@NotNull Long rowVersion, String idempotencyKey) {}

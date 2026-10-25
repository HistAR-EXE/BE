package com.histar.be.siteworkspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContentRollbackRequest(@NotBlank String reason, @NotNull Long expectedRevision) {}

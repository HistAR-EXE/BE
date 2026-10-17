package com.histar.be.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForceVerifyRequest(@NotBlank @Size(min = 10, max = 1000) String reason) {}

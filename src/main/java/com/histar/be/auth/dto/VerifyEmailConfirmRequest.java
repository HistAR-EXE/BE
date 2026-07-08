package com.histar.be.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailConfirmRequest(@NotBlank String token) {}

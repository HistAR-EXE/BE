package com.histar.be.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmDto(
        @NotBlank String resetToken, @NotBlank @Size(min = 6, max = 128) String password) {}

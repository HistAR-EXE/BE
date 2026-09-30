package com.histar.be.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterPushTokenRequest(
        @NotBlank @Size(max = 512) String token,
        @NotBlank @Pattern(regexp = "ANDROID|IOS|WEB") String platform) {}

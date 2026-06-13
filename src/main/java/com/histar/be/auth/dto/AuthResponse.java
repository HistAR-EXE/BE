package com.histar.be.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        String accessToken,
        Long expiresIn,
        String refreshToken,
        Long refreshExpiresIn,
        UUID userId,
        String displayName,
        String role) {}

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
        String email,
        String role,
        String tier,
        UUID orgId,
        String orgSubscription,
        boolean emailVerified,
        String debugVerificationToken) {}

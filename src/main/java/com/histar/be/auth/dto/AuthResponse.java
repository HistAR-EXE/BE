package com.histar.be.auth.dto;

import java.util.UUID;

public record AuthResponse(String token, UUID userId, String displayName) {
}

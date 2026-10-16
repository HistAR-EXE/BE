package com.histar.be.admin.dto;

import java.util.UUID;

public record CreateAdminUserResponse(
        UUID id,
        String email,
        String displayName,
        String role,
        boolean accountCreated,
        boolean credentialsEmailed,
        String temporaryPassword) {}

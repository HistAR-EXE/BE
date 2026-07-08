package com.histar.be.organization.dto;

import java.util.UUID;

public record ProvisionStudentResponse(
        UUID userId,
        String email,
        String displayName,
        boolean accountCreated,
        boolean credentialsEmailed,
        String temporaryPassword) {}

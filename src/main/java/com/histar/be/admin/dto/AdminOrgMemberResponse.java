package com.histar.be.admin.dto;

import java.util.UUID;

public record AdminOrgMemberResponse(
        UUID userId,
        String email,
        String displayName,
        boolean accountCreated,
        boolean credentialsEmailed,
        String temporaryPassword) {}

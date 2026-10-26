package com.histar.be.admin.dto;

import java.util.UUID;

public record AdminDemoAccountResponse(
        UUID id,
        int persona,
        String email,
        String displayName,
        String role,
        String tier,
        long points) {}

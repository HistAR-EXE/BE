package com.histar.be.admin.dto;

import java.util.UUID;

public record AdminResendVerificationResponse(
        UUID userId, boolean emailed, String debugVerificationToken) {}

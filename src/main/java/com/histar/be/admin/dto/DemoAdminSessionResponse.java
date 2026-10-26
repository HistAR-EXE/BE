package com.histar.be.admin.dto;

import java.time.Instant;

public record DemoAdminSessionResponse(String url, Instant expiresAt) {}

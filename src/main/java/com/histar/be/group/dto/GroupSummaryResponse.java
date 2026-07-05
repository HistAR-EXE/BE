package com.histar.be.group.dto;

import java.time.Instant;
import java.util.UUID;

public record GroupSummaryResponse(UUID id, String name, String code, Instant expiresAt, int memberCount) {}

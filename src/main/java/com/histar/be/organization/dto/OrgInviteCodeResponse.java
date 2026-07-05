package com.histar.be.organization.dto;

import java.time.Instant;
import java.util.UUID;

public record OrgInviteCodeResponse(String inviteCode, Instant expiresAt, UUID organizationId) {}

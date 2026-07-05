package com.histar.be.group.dto;

import java.util.UUID;

public record GroupMemberResponse(UUID userId, String displayName, String avatarUrl) {}

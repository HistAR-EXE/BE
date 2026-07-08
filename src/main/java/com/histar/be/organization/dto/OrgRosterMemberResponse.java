package com.histar.be.organization.dto;

import java.util.List;
import java.util.UUID;

public record OrgRosterMemberResponse(
        UUID userId,
        String displayName,
        String email,
        String orgRole,
        int level,
        int totalPoints,
        long questsCompleted,
        List<OrgQuestProgressItem> questProgress) {}

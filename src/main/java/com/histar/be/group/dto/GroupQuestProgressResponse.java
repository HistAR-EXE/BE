package com.histar.be.group.dto;

import java.util.List;
import java.util.UUID;

public record GroupQuestProgressResponse(
        UUID questId,
        String questTitle,
        List<GroupMemberQuestProgressResponse> members) {}

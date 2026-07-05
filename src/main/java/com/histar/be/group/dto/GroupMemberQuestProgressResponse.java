package com.histar.be.group.dto;

import java.util.UUID;

public record GroupMemberQuestProgressResponse(
        UUID userId,
        String displayName,
        String status,
        Integer currentStep,
        Integer stepsTotal,
        Integer progressPercent) {}

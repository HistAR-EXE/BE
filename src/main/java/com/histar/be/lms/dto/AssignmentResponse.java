package com.histar.be.lms.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        String title,
        UUID questId,
        String questTitle,
        Instant dueAt,
        Instant createdAt,
        List<AssignmentSubmissionResponse> submissions) {}

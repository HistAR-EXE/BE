package com.histar.be.lms.dto;

import java.time.Instant;
import java.util.UUID;

public record AssignmentSubmissionResponse(
        UUID id,
        UUID studentId,
        String studentName,
        Integer score,
        boolean autoGraded,
        Instant completedAt) {}

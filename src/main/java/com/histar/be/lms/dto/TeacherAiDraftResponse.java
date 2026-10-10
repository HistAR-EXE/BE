package com.histar.be.lms.dto;

import com.histar.be.chat.guided.GuidedAnswerBlock;
import com.histar.be.lms.entity.TeacherAiDraftStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record TeacherAiDraftResponse(
        UUID id,
        String title,
        UUID questId,
        UUID campaignId,
        String sourceVersion,
        List<GuidedAnswerBlock> answerBlocks,
        Set<String> approvedLocales,
        boolean evaluationPassed,
        List<String> evaluationReasons,
        TeacherAiDraftStatus status,
        Instant dueAt,
        UUID publishedAssignmentId,
        Instant createdAt,
        Instant updatedAt) {}

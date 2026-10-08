package com.histar.be.lms.dto;

import com.histar.be.chat.guided.GuidedAnswerBlock;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CreateTeacherAiDraftRequest(
        @NotBlank String title,
        @NotNull UUID questId,
        @NotBlank String sourceVersion,
        @NotNull List<GuidedAnswerBlock> answerBlocks,
        @NotNull Set<String> approvedLocales,
        Instant dueAt) {}

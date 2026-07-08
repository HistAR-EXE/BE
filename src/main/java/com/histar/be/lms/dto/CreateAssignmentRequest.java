package com.histar.be.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateAssignmentRequest(
        @NotBlank String title, @NotNull UUID questId, Instant dueAt) {}

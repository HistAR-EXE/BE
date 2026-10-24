package com.histar.be.siteworkspace.dto;

import jakarta.validation.constraints.NotNull;

public record ContentReviewRequest(
        @NotNull ContentReviewDecision decision, String note, @NotNull Long expectedRevision) {}

package com.histar.be.admin.contentreport;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateContentReportRequest(
        @NotNull ContentReportStatus status,
        @Size(max = 4000) String adminNote,
        @NotNull @PositiveOrZero Long rowVersion) {}

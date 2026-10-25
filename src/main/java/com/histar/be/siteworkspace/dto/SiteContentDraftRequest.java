package com.histar.be.siteworkspace.dto;

import jakarta.validation.constraints.NotNull;

public record SiteContentDraftRequest(@NotNull Object snapshot, Long expectedRevision) {}

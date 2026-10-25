package com.histar.be.siteworkspace.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record SiteContentDraftRequest(@NotNull JsonNode snapshot, Long expectedRevision) {}

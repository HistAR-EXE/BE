package com.histar.be.fieldtrip.dto;

import jakarta.validation.constraints.NotNull;

public record SiteTripDecisionRequest(@NotNull Long rowVersion, String note) {}

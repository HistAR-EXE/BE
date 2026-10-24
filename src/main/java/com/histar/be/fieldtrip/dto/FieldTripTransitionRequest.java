package com.histar.be.fieldtrip.dto;

import jakarta.validation.constraints.NotNull;

public record FieldTripTransitionRequest(@NotNull Long rowVersion) {}

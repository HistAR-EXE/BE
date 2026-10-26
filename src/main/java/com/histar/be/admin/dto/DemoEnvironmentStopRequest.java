package com.histar.be.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record DemoEnvironmentStopRequest(@NotBlank String confirmation) {}

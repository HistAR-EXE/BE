package com.histar.be.organization.dto;

import jakarta.validation.constraints.NotNull;

public record LeaveOrgRequest(@NotNull Boolean confirm) {}

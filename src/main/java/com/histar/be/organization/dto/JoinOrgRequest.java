package com.histar.be.organization.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinOrgRequest(@NotBlank String inviteCode) {}

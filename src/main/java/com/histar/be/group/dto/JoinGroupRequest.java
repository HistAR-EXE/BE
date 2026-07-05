package com.histar.be.group.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinGroupRequest(@NotBlank String code) {}

package com.histar.be.squad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinSquadRequest(@NotBlank @Size(min = 6, max = 6) String code) {}

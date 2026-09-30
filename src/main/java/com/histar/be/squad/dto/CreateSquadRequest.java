package com.histar.be.squad.dto;

import jakarta.validation.constraints.Size;

public record CreateSquadRequest(@Size(max = 64) String siteCode) {}

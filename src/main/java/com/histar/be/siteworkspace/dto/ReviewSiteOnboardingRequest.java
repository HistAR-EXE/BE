package com.histar.be.siteworkspace.dto;

import jakarta.validation.constraints.Size;

public record ReviewSiteOnboardingRequest(@Size(max = 4000) String note) {}

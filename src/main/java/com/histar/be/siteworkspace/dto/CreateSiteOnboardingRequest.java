package com.histar.be.siteworkspace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSiteOnboardingRequest(
        @NotBlank @Size(max = 255) String representativeName,
        @NotBlank @Size(max = 255) String organizationName,
        @NotBlank @Email @Size(max = 320) String representativeEmail,
        @NotBlank @Size(max = 64) String siteCode,
        @NotBlank @Size(max = 4000) String evidenceNote) {}

package com.histar.be.profile.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 80) String displayName,
        @Size(max = 512) String avatarUrl,
        @Size(max = 120) String city) {}

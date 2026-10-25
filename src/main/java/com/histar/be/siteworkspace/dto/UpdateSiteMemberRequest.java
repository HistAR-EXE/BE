package com.histar.be.siteworkspace.dto;

import com.histar.be.siteworkspace.entity.SiteRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateSiteMemberRequest(
        @NotNull UUID userId,
        @NotNull SiteRole role) {}

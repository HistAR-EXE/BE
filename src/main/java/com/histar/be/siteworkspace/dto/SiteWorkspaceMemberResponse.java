package com.histar.be.siteworkspace.dto;

import com.histar.be.siteworkspace.entity.SiteRole;
import java.util.UUID;

public record SiteWorkspaceMemberResponse(
        UUID userId,
        SiteRole role,
        boolean active) {}

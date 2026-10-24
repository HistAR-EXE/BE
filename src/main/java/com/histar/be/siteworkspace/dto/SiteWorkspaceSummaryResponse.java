package com.histar.be.siteworkspace.dto;

import com.histar.be.siteworkspace.entity.SiteRole;
import java.util.List;

public record SiteWorkspaceSummaryResponse(
        String siteCode,
        String locationName,
        SiteRole callerRole,
        boolean requiresTripConfirmation,
        List<SiteWorkspaceMemberResponse> members,
        String name,
        String description,
        String formattedAddress,
        String coverImage,
        String sources) {}

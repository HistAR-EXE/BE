package com.histar.be.siteworkspace.dto;

import com.histar.be.siteworkspace.entity.SiteRole;

public record SiteWorkspaceMembershipResponse(
        String siteCode,
        String locationName,
        SiteRole role,
        boolean requiresTripConfirmation) {}

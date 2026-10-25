package com.histar.be.siteworkspace.dto;

public record SiteWorkspaceSettingsResponse(
        String siteCode,
        boolean requiresTripConfirmation) {}

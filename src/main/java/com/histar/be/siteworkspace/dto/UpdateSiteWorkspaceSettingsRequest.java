package com.histar.be.siteworkspace.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSiteWorkspaceSettingsRequest(
        @NotNull Boolean requiresTripConfirmation) {}

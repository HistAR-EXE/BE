package com.histar.be.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record ExchangeDemoAdminSessionRequest(@NotBlank String ticket) {}

package com.histar.be.admin.dto;

import java.util.List;

public record AdminDemoDatasetResponse(
        boolean enabled,
        long accountCount,
        long operationalRowCount,
        List<AdminDemoAccountResponse> accounts) {}

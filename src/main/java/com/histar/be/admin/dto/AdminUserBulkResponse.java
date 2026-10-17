package com.histar.be.admin.dto;

import java.util.List;
import java.util.UUID;

public record AdminUserBulkResponse(int succeeded, List<Failure> failed) {

    public record Failure(UUID userId, String message) {}
}

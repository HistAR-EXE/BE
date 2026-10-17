package com.histar.be.admin.trash;

import java.util.List;
import java.util.UUID;

public record AdminTrashBulkResult(int succeeded, int failedCount, List<Failure> failed) {

    public record Failure(String resourceType, UUID id, String message) {}
}

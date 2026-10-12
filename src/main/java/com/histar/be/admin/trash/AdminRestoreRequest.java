package com.histar.be.admin.trash;

public record AdminRestoreRequest(Long rowVersion, String idempotencyKey) {}

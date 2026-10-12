package com.histar.be.admin.trash;

public record AdminDeleteRequest(String reasonPreset, String reasonDetail, Long rowVersion, String idempotencyKey) {}

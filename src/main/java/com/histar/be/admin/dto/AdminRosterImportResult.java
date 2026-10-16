package com.histar.be.admin.dto;

import java.util.List;

public record AdminRosterImportResult(
        int created, int enrolled, int skipped, List<String> errors) {}

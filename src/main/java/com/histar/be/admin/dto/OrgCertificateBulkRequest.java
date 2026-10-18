package com.histar.be.admin.dto;

import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record OrgCertificateBulkRequest(
        @Size(max = 200) List<UUID> userIds, @Size(max = 255) String title) {}

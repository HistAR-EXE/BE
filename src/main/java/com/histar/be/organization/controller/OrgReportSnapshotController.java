package com.histar.be.organization.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.organization.dto.OrgReportSnapshotResponse;
import com.histar.be.organization.service.OrgReportSnapshotService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/org/{orgId}/report-snapshots")
@RequiredArgsConstructor
public class OrgReportSnapshotController {
    private final OrgReportSnapshotService snapshots;
    private final CurrentUserAccessor currentUser;

    @GetMapping
    public ApiResponse<List<OrgReportSnapshotResponse>> list(@PathVariable UUID orgId) {
        return ApiResponse.ok(snapshots.list(actor(), orgId));
    }

    @PostMapping
    public ApiResponse<OrgReportSnapshotResponse> capture(@PathVariable UUID orgId) {
        return ApiResponse.ok(snapshots.capture(actor(), orgId));
    }

    private UUID actor() {
        return currentUser.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
    }
}

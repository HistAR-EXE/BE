package com.histar.be.admin.controller;

import com.histar.be.admin.dto.AdminOrgMemberRequest;
import com.histar.be.admin.dto.AdminOrgMemberResponse;
import com.histar.be.admin.dto.AdminOrganizationSummaryResponse;
import com.histar.be.admin.dto.AdminRosterImportResult;
import com.histar.be.admin.dto.CreateAdminOrganizationRequest;
import com.histar.be.admin.dto.UpdateAdminOrganizationRequest;
import com.histar.be.admin.service.AdminOrganizationService;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.response.PageResponse;
import com.histar.be.organization.dto.OrgInviteCodeResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
public class AdminOrganizationController {

    private final AdminOrganizationService adminOrganizationService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<AdminOrganizationSummaryResponse>> listOrganizations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(adminOrganizationService.list(page, size, q, plan, status));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminOrganizationSummaryResponse> create(
            @RequestBody @Valid CreateAdminOrganizationRequest request) {
        return ApiResponse.ok(adminOrganizationService.create(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminOrganizationSummaryResponse> update(
            @PathVariable UUID id, @RequestBody @Valid UpdateAdminOrganizationRequest request) {
        return ApiResponse.ok(adminOrganizationService.update(id, request));
    }

    @PostMapping("/{id}/invite-code")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrgInviteCodeResponse> generateInvite(@PathVariable UUID id) {
        return ApiResponse.ok(adminOrganizationService.generateInvite(id));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminOrgMemberResponse> addMember(
            @PathVariable UUID id, @RequestBody @Valid AdminOrgMemberRequest request) {
        return ApiResponse.ok(adminOrganizationService.addMember(id, request));
    }

    @GetMapping(value = "/{id}/roster/export.csv", produces = "text/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportRoster(@PathVariable UUID id) {
        byte[] body = adminOrganizationService.exportRosterCsv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=" + StandardCharsets.UTF_8.name())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"org-roster-" + id + ".csv\"")
                .body(body);
    }

    @PostMapping("/{id}/roster/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminRosterImportResult> importRoster(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(adminOrganizationService.importRosterCsv(id, file));
    }
}

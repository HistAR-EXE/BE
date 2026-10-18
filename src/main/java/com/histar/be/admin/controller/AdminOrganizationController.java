package com.histar.be.admin.controller;

import com.histar.be.admin.audit.AdminAuditEventResponse;
import com.histar.be.admin.dto.AdminOrgMemberRequest;
import com.histar.be.admin.dto.AdminOrgMemberResponse;
import com.histar.be.admin.dto.AdminOrganizationSummaryResponse;
import com.histar.be.admin.dto.AdminRosterImportResult;
import com.histar.be.admin.dto.CreateAdminOrganizationRequest;
import com.histar.be.admin.dto.OrgArCardIssueRequest;
import com.histar.be.admin.dto.OrgArCardIssueResult;
import com.histar.be.admin.dto.OrgArCardResponse;
import com.histar.be.admin.dto.OrgArReportResponse;
import com.histar.be.admin.dto.OrgCertificateBulkRequest;
import com.histar.be.admin.dto.OrgCertificateBulkResult;
import com.histar.be.admin.dto.OrgCertificateResponse;
import com.histar.be.admin.dto.OrgContractRequest;
import com.histar.be.admin.dto.OrgContractResponse;
import com.histar.be.admin.dto.OrgCrmSyncResponse;
import com.histar.be.admin.dto.UpdateAdminOrganizationRequest;
import com.histar.be.admin.service.AdminOrgOpsService;
import com.histar.be.admin.service.AdminOrganizationService;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.response.PageResponse;
import com.histar.be.organization.dto.OrgInviteCodeResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final AdminOrgOpsService adminOrgOpsService;

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

    @GetMapping("/{id}/contracts")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<OrgContractResponse>> listContracts(@PathVariable UUID id) {
        return ApiResponse.ok(adminOrgOpsService.listContracts(id));
    }

    @PostMapping("/{id}/contracts")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrgContractResponse> createContract(
            @PathVariable UUID id, @RequestBody @Valid OrgContractRequest request) {
        return ApiResponse.ok(adminOrgOpsService.createContract(id, request));
    }

    @PatchMapping("/{id}/contracts/{contractId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrgContractResponse> updateContract(
            @PathVariable UUID id,
            @PathVariable UUID contractId,
            @RequestBody @Valid OrgContractRequest request) {
        return ApiResponse.ok(adminOrgOpsService.updateContract(id, contractId, request));
    }

    @DeleteMapping("/{id}/contracts/{contractId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrgContractResponse> cancelContract(
            @PathVariable UUID id, @PathVariable UUID contractId) {
        return ApiResponse.ok(adminOrgOpsService.cancelContract(id, contractId));
    }

    @GetMapping("/{id}/ar-report")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrgArReportResponse> arReport(
            @PathVariable UUID id,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return ApiResponse.ok(adminOrgOpsService.arReport(id, from, to));
    }

    @GetMapping("/{id}/activity-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<AdminAuditEventResponse>> activityLogs(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminOrgOpsService.activityLogs(id, page, size));
    }

    @PostMapping("/{id}/sync-crm")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrgCrmSyncResponse> syncCrm(@PathVariable UUID id) {
        return ApiResponse.ok(adminOrgOpsService.syncCrm(id));
    }

    @GetMapping("/{id}/ar-cards")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<OrgArCardResponse>> listArCards(@PathVariable UUID id) {
        return ApiResponse.ok(adminOrgOpsService.listArCards(id));
    }

    @PostMapping("/{id}/ar-cards")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrgArCardIssueResult> issueArCards(
            @PathVariable UUID id, @RequestBody(required = false) OrgArCardIssueRequest request) {
        return ApiResponse.ok(adminOrgOpsService.issueArCards(id, request));
    }

    @PatchMapping("/{id}/ar-cards/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrgArCardResponse> revokeArCard(
            @PathVariable UUID id, @PathVariable UUID cardId) {
        return ApiResponse.ok(adminOrgOpsService.revokeArCard(id, cardId));
    }

    @GetMapping("/{id}/certificates")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<OrgCertificateResponse>> listCertificates(@PathVariable UUID id) {
        return ApiResponse.ok(adminOrgOpsService.listCertificates(id));
    }

    @PostMapping("/{id}/certificates/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrgCertificateBulkResult> issueCertificates(
            @PathVariable UUID id, @RequestBody(required = false) OrgCertificateBulkRequest request) {
        return ApiResponse.ok(adminOrgOpsService.issueCertificates(id, request == null
                ? new OrgCertificateBulkRequest(null, null)
                : request));
    }

    @GetMapping(value = "/{id}/certificates/export.csv", produces = "text/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportCertificates(@PathVariable UUID id) {
        byte[] body = adminOrgOpsService.exportCertificatesCsv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=" + StandardCharsets.UTF_8.name())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"org-certificates-" + id + ".csv\"")
                .body(body);
    }
}

package com.histar.be.admin.controller;

import com.histar.be.admin.dto.AdminResendVerificationResponse;
import com.histar.be.admin.dto.AdminUserBulkRequest;
import com.histar.be.admin.dto.AdminUserBulkResponse;
import com.histar.be.admin.dto.AdminUserSummaryResponse;
import com.histar.be.admin.dto.CreateAdminUserRequest;
import com.histar.be.admin.dto.CreateAdminUserResponse;
import com.histar.be.admin.dto.ForceVerifyRequest;
import com.histar.be.admin.dto.UpdateUserRoleRequest;
import com.histar.be.admin.dto.UpdateUserStatusRequest;
import com.histar.be.admin.dto.UpdateUserTierRequest;
import com.histar.be.admin.service.AdminUserService;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.response.PageResponse;
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

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<AdminUserSummaryResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(adminUserService.listUsers(page, size, q, role, tier, verified, status));
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ids) {
        byte[] body = adminUserService.exportUsersCsv(q, role, tier, verified, status, ids);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=" + StandardCharsets.UTF_8.name())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"admin-users.csv\"")
                .body(body);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CreateAdminUserResponse> createUser(
            @RequestBody @Valid CreateAdminUserRequest request) {
        return ApiResponse.ok(adminUserService.createUser(request));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserBulkResponse> bulk(@RequestBody @Valid AdminUserBulkRequest request) {
        return ApiResponse.ok(adminUserService.bulk(request));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserSummaryResponse> updateRole(
            @PathVariable UUID id, @RequestBody @Valid UpdateUserRoleRequest request) {
        return ApiResponse.ok(adminUserService.updateRole(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserSummaryResponse> updateStatus(
            @PathVariable UUID id, @RequestBody @Valid UpdateUserStatusRequest request) {
        return ApiResponse.ok(adminUserService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/tier")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserSummaryResponse> updateTier(
            @PathVariable UUID id, @RequestBody @Valid UpdateUserTierRequest request) {
        return ApiResponse.ok(adminUserService.updateTier(id, request));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserSummaryResponse> forceVerify(
            @PathVariable UUID id, @RequestBody @Valid ForceVerifyRequest request) {
        return ApiResponse.ok(adminUserService.forceVerify(id, request));
    }

    @PostMapping("/{id}/resend-verification")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminResendVerificationResponse> resendVerification(@PathVariable UUID id) {
        return ApiResponse.ok(adminUserService.resendVerification(id));
    }
}

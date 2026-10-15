package com.histar.be.admin.controller;

import com.histar.be.admin.dto.AdminUserSummaryResponse;
import com.histar.be.admin.dto.CreateAdminUserRequest;
import com.histar.be.admin.dto.CreateAdminUserResponse;
import com.histar.be.admin.dto.UpdateUserRoleRequest;
import com.histar.be.admin.service.AdminUserService;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.response.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
            @RequestParam(required = false) Boolean verified) {
        return ApiResponse.ok(adminUserService.listUsers(page, size, q, role, tier, verified));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CreateAdminUserResponse> createUser(
            @RequestBody @Valid CreateAdminUserRequest request) {
        return ApiResponse.ok(adminUserService.createUser(request));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserSummaryResponse> updateRole(
            @PathVariable UUID id, @RequestBody @Valid UpdateUserRoleRequest request) {
        return ApiResponse.ok(adminUserService.updateRole(id, request));
    }
}

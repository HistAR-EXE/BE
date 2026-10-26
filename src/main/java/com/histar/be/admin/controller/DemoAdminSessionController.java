package com.histar.be.admin.controller;

import com.histar.be.admin.dto.DemoAdminSessionResponse;
import com.histar.be.admin.dto.ExchangeDemoAdminSessionRequest;
import com.histar.be.admin.service.DemoAdminSessionService;
import com.histar.be.auth.dto.AuthResponse;
import com.histar.be.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DemoAdminSessionController {
    private final DemoAdminSessionService service;

    @PostMapping("/admin/demo-session")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DemoAdminSessionResponse> issue(Authentication authentication) {
        return ApiResponse.ok(service.issue(authentication.getName()));
    }

    @PostMapping("/demo/admin-session/exchange")
    public ApiResponse<AuthResponse> exchange(@RequestBody @Valid ExchangeDemoAdminSessionRequest request) {
        return ApiResponse.ok(service.exchange(request.ticket()));
    }
}

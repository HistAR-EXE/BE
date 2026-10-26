package com.histar.be.admin.controller;

import com.histar.be.admin.dto.DemoEnvironmentStatus;
import com.histar.be.admin.dto.DemoEnvironmentStopRequest;
import com.histar.be.admin.service.DemoEnvironmentService;
import com.histar.be.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/demo-environment")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDemoEnvironmentController {
    private final DemoEnvironmentService service;

    @GetMapping
    public ApiResponse<DemoEnvironmentStatus> status() {
        return ApiResponse.ok(service.status());
    }

    @PostMapping("/start")
    public ApiResponse<DemoEnvironmentStatus> start() {
        return ApiResponse.ok(service.start());
    }

    @PostMapping("/stop")
    public ApiResponse<DemoEnvironmentStatus> stop(
            @RequestBody @Valid DemoEnvironmentStopRequest request) {
        return ApiResponse.ok(service.stop(request.confirmation()));
    }
}

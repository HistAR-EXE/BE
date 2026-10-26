package com.histar.be.admin.controller;

import com.histar.be.admin.dto.AdminDemoDatasetResponse;
import com.histar.be.admin.dto.PurgeDemoDatasetRequest;
import com.histar.be.admin.dto.PurgeDemoDatasetResponse;
import com.histar.be.admin.service.AdminDemoDatasetService;
import com.histar.be.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/demo-dataset")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDemoDatasetController {

    private final AdminDemoDatasetService service;

    @GetMapping
    public ApiResponse<AdminDemoDatasetResponse> preview() {
        return ApiResponse.ok(service.preview());
    }

    @DeleteMapping
    public ApiResponse<PurgeDemoDatasetResponse> purge(
            @RequestBody @Valid PurgeDemoDatasetRequest request) {
        return ApiResponse.ok(service.purge(request.userId(), request.reason()));
    }
}

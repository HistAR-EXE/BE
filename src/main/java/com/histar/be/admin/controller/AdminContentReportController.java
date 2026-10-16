package com.histar.be.admin.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.admin.contentreport.AdminContentReportService;
import com.histar.be.admin.contentreport.ContentReportResponse;
import com.histar.be.admin.contentreport.ContentReportStatus;
import com.histar.be.admin.contentreport.UpdateContentReportRequest;
import java.time.Instant;
import java.util.UUID;
import com.histar.be.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/content-reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentReportController {

    private final AdminContentReportService service;

    @GetMapping
    public ApiResponse<PageResponse<ContentReportResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ContentReportStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ApiResponse.ok(service.list(page, size, status, q, from, to));
    }

    @PatchMapping("/{eventId}")
    public ApiResponse<ContentReportResponse> update(
            @PathVariable UUID eventId, @RequestBody @Valid UpdateContentReportRequest request) {
        return ApiResponse.ok(service.update(eventId, request));
    }
}

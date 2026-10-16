package com.histar.be.admin.controller;

import com.histar.be.billing.dto.AdminCreateB2b2cInquiryRequest;
import com.histar.be.billing.dto.B2b2cInquiryItem;
import com.histar.be.billing.dto.B2b2cInquiryStatusUpdateRequest;
import com.histar.be.billing.service.B2b2cInquiryService;
import com.histar.be.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import java.time.Instant;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/b2b2c-inquiries", "/api/billing/admin/b2b2c-inquiries"})
@RequiredArgsConstructor
public class AdminB2b2cInquiryController {

    private final B2b2cInquiryService b2b2cInquiryService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> listAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        if (page == null) {
            return ApiResponse.ok(b2b2cInquiryService.listAll());
        }
        return ApiResponse.ok(b2b2cInquiryService.listPaged(page, size, status, q, from, to));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<B2b2cInquiryItem> create(@RequestBody @Valid AdminCreateB2b2cInquiryRequest request) {
        return ApiResponse.ok(b2b2cInquiryService.createAsAdmin(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<B2b2cInquiryItem> updateStatus(
            @PathVariable UUID id, @RequestBody @Valid B2b2cInquiryStatusUpdateRequest request) {
        return ApiResponse.ok(b2b2cInquiryService.updateStatus(id, request.status(), request.adminNotes()));
    }
}

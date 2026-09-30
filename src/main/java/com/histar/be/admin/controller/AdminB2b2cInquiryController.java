package com.histar.be.admin.controller;

import com.histar.be.billing.dto.B2b2cInquiryItem;
import com.histar.be.billing.dto.B2b2cInquiryStatusUpdateRequest;
import com.histar.be.billing.service.B2b2cInquiryService;
import com.histar.be.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/b2b2c-inquiries")
@RequiredArgsConstructor
public class AdminB2b2cInquiryController {

    private final B2b2cInquiryService b2b2cInquiryService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<B2b2cInquiryItem>> listAll() {
        return ApiResponse.ok(b2b2cInquiryService.listAll());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<B2b2cInquiryItem> updateStatus(
            @PathVariable UUID id, @RequestBody @Valid B2b2cInquiryStatusUpdateRequest request) {
        return ApiResponse.ok(b2b2cInquiryService.updateStatus(id, request.status(), request.adminNotes()));
    }
}

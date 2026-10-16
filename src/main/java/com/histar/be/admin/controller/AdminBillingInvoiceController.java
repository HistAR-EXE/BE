package com.histar.be.admin.controller;

import com.histar.be.billing.dto.AdminCreateB2bInvoiceRequest;
import com.histar.be.billing.dto.OrgPaymentIntentResponse;
import com.histar.be.billing.service.SepayOrgPaymentService;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/billing/invoices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBillingInvoiceController {

    private final SepayOrgPaymentService sepayOrgPaymentService;
    private final CurrentUserAccessor currentUser;

    @PostMapping
    public ApiResponse<OrgPaymentIntentResponse> create(@RequestBody @Valid AdminCreateB2bInvoiceRequest request) {
        UUID adminUserId = currentUser.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(sepayOrgPaymentService.createAdminInvoice(adminUserId, request));
    }
}

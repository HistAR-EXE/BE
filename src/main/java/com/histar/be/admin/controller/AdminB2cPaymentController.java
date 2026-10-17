package com.histar.be.admin.controller;

import com.histar.be.billing.dto.AdminRecentB2cPaymentItem;
import com.histar.be.billing.dto.AdminPaymentOperationRequest;
import com.histar.be.billing.dto.AdminPaymentOperationResponse;
import com.histar.be.billing.service.AdminB2cPaymentQueryService;
import com.histar.be.billing.service.AdminPaymentOpsService;
import com.histar.be.billing.service.AdminPaymentQueryService;
import com.histar.be.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/billing/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminB2cPaymentController {

    private final AdminB2cPaymentQueryService adminB2cPaymentQueryService;
    private final AdminPaymentQueryService adminPaymentQueryService;
    private final AdminPaymentOpsService adminPaymentOpsService;

    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AdminRecentB2cPaymentItem>> recentPayments(
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(adminB2cPaymentQueryService.listRecent(limit));
    }

    /** e.g. {@code GET /api/admin/billing/payments?status=UNDERPAID} */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> listPayments(
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "50") int limit) {
        if (page == null) {
            return ApiResponse.ok(adminB2cPaymentQueryService.listByStatus(status, limit));
        }
        return ApiResponse.ok(adminPaymentQueryService.search(page, size, channel, status, from, to, sort));
    }

    @PostMapping("/b2c/{id}/mark-paid")
    public ApiResponse<AdminPaymentOperationResponse> markB2cPaid(
            @PathVariable UUID id, @RequestBody @Valid AdminPaymentOperationRequest request) {
        return ApiResponse.ok(adminPaymentOpsService.markPaid("B2C", id, request.reason()));
    }

    @PostMapping("/b2c/{id}/refund")
    public ApiResponse<AdminPaymentOperationResponse> refundB2c(
            @PathVariable UUID id, @RequestBody @Valid AdminPaymentOperationRequest request) {
        return ApiResponse.ok(adminPaymentOpsService.refund("B2C", id, request.reason()));
    }

    @PostMapping("/b2c/{id}/void")
    public ApiResponse<AdminPaymentOperationResponse> voidB2c(
            @PathVariable UUID id, @RequestBody @Valid AdminPaymentOperationRequest request) {
        return ApiResponse.ok(adminPaymentOpsService.voidPayment("B2C", id, request.reason()));
    }

    @PostMapping("/b2b/{id}/mark-paid")
    public ApiResponse<AdminPaymentOperationResponse> markB2bPaid(
            @PathVariable UUID id, @RequestBody @Valid AdminPaymentOperationRequest request) {
        return ApiResponse.ok(adminPaymentOpsService.markPaid("B2B", id, request.reason()));
    }

    @PostMapping("/b2b/{id}/refund")
    public ApiResponse<AdminPaymentOperationResponse> refundB2b(
            @PathVariable UUID id, @RequestBody @Valid AdminPaymentOperationRequest request) {
        return ApiResponse.ok(adminPaymentOpsService.refund("B2B", id, request.reason()));
    }

    @PostMapping("/b2b/{id}/void")
    public ApiResponse<AdminPaymentOperationResponse> voidB2b(
            @PathVariable UUID id, @RequestBody @Valid AdminPaymentOperationRequest request) {
        return ApiResponse.ok(adminPaymentOpsService.voidPayment("B2B", id, request.reason()));
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        byte[] csv = adminPaymentQueryService.exportCsv(channel, status, from, to, sort)
                .getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payments.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }
}

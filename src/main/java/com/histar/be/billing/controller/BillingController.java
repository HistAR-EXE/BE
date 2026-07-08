package com.histar.be.billing.controller;

import com.histar.be.billing.dto.B2cSubscribeRequest;
import com.histar.be.billing.dto.B2b2cInquiryItem;
import com.histar.be.billing.dto.B2b2cInquiryRequest;
import com.histar.be.billing.dto.B2b2cInquiryResponse;
import com.histar.be.billing.dto.B2cSubscribeRequest;
import com.histar.be.billing.dto.OrgVolumePreviewResponse;
import com.histar.be.billing.OrgPlanLimits;
import com.histar.be.organization.entity.OrgSubscription;
import com.histar.be.billing.dto.B2cCreatePaymentRequest;
import com.histar.be.billing.dto.B2cPaymentIntentResponse;
import com.histar.be.billing.dto.B2cPaymentStatusResponse;
import com.histar.be.billing.dto.B2cSubscriptionHistoryItem;
import com.histar.be.billing.dto.BillingPublicPricingResponse;
import com.histar.be.billing.dto.BillingStatusResponse;
import com.histar.be.billing.dto.OrgCreatePaymentRequest;
import com.histar.be.billing.dto.OrgPlanInfo;
import com.histar.be.billing.dto.OrgBillingStatus;
import com.histar.be.billing.dto.OrgPaymentIntentResponse;
import com.histar.be.billing.dto.OrgPaymentStatusResponse;
import com.histar.be.billing.dto.OrgSubscriptionHistoryItem;
import com.histar.be.billing.dto.OrgSubscribeRequest;
import com.histar.be.billing.dto.OrgTrialRequest;
import com.histar.be.billing.service.B2b2cInquiryService;
import com.histar.be.billing.service.BillingService;
import com.histar.be.billing.service.BillingSettingsService;
import com.histar.be.billing.service.SepayB2cPaymentService;
import com.histar.be.billing.service.SepayOrgPaymentService;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.profile.dto.ProfileMeResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final SepayB2cPaymentService sepayB2cPaymentService;
    private final SepayOrgPaymentService sepayOrgPaymentService;
    private final B2b2cInquiryService b2b2cInquiryService;
    private final BillingSettingsService billingSettingsService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping("/b2c/subscribe")
    public ApiResponse<ProfileMeResponse> subscribeB2c(@RequestBody(required = false) B2cSubscribeRequest request) {
        UUID userId = requireUserId();
        String paymentMethod = request != null ? request.paymentMethod() : "DEMO";
        return ApiResponse.ok(billingService.subscribeB2c(userId, paymentMethod));
    }

    @GetMapping("/b2c/status")
    public ApiResponse<com.histar.be.billing.dto.B2cBillingStatus> b2cStatus() {
        return ApiResponse.ok(billingService.getB2cStatus(requireUserId()));
    }

    @DeleteMapping("/b2c/cancel")
    public ApiResponse<com.histar.be.billing.dto.B2cBillingStatus> cancelB2c() {
        return ApiResponse.ok(billingService.cancelB2c(requireUserId()));
    }

    @GetMapping("/b2c/history")
    public ApiResponse<List<B2cSubscriptionHistoryItem>> b2cHistory() {
        return ApiResponse.ok(billingService.getB2cHistory(requireUserId()));
    }

    @PostMapping("/b2c/payment")
    public ApiResponse<B2cPaymentIntentResponse> createB2cPayment(
            @RequestBody(required = false) B2cCreatePaymentRequest request) {
        String returnToPath = request != null ? request.returnToPath() : null;
        return ApiResponse.ok(sepayB2cPaymentService.createPayment(requireUserId(), returnToPath));
    }

    @GetMapping("/b2c/payment/{orderCode}")
    public ApiResponse<B2cPaymentStatusResponse> getB2cPaymentStatus(@PathVariable String orderCode) {
        return ApiResponse.ok(sepayB2cPaymentService.getPaymentStatus(requireUserId(), orderCode));
    }

    @PostMapping("/org/subscribe")
    public ApiResponse<OrgBillingStatus> subscribeOrg(@RequestBody @Valid OrgSubscribeRequest request) {
        UUID userId = requireUserId();
        return ApiResponse.ok(billingService.subscribeOrg(userId, request));
    }

    @PostMapping("/org/trial")
    public ApiResponse<OrgBillingStatus> createOrgTrial(@RequestBody(required = false) OrgTrialRequest request) {
        UUID userId = requireUserId();
        return ApiResponse.ok(billingService.createOrgTrial(userId, request));
    }

    @GetMapping("/status")
    public ApiResponse<BillingStatusResponse> status() {
        return ApiResponse.ok(billingService.getStatus(requireUserId()));
    }

    @GetMapping("/org/plans")
    public ApiResponse<List<OrgPlanInfo>> orgPlans() {
        return ApiResponse.ok(billingService.listOrgPlans());
    }

    @GetMapping("/public-pricing")
    public ApiResponse<BillingPublicPricingResponse> publicPricing() {
        return ApiResponse.ok(billingService.getPublicPricing());
    }

    @GetMapping("/org/status")
    public ApiResponse<OrgBillingStatus> orgStatus() {
        return ApiResponse.ok(billingService.getOrgStatus(requireUserId()));
    }

    @GetMapping("/org/history")
    public ApiResponse<List<OrgSubscriptionHistoryItem>> orgHistory() {
        return ApiResponse.ok(billingService.getOrgHistory(requireUserId()));
    }

    @PostMapping("/org/payment")
    public ApiResponse<OrgPaymentIntentResponse> createOrgPayment(@RequestBody @Valid OrgCreatePaymentRequest request) {
        return ApiResponse.ok(sepayOrgPaymentService.createPayment(requireUserId(), request));
    }

    @GetMapping("/org/payment/{orderCode}")
    public ApiResponse<OrgPaymentStatusResponse> getOrgPaymentStatus(@PathVariable String orderCode) {
        return ApiResponse.ok(sepayOrgPaymentService.getPaymentStatus(requireUserId(), orderCode));
    }

    @PostMapping("/webhooks/sepay")
    public ResponseEntity<ApiResponse<Void>> sepayWebhook(
            @RequestHeader(value = "X-SePay-Signature", required = false) String signature,
            @RequestHeader(value = "X-SePay-Timestamp", required = false) String timestamp,
            @RequestBody String rawBody) {
        sepayB2cPaymentService.handleWebhook(timestamp, signature, rawBody);
        sepayOrgPaymentService.handleWebhook(timestamp, signature, rawBody);
        return ResponseEntity.ok(ApiResponse.ok("Webhook received", null));
    }

    @PostMapping("/b2b2c-inquiry")
    public ApiResponse<B2b2cInquiryResponse> submitB2b2cInquiry(@RequestBody @Valid B2b2cInquiryRequest request) {
        return ApiResponse.ok(b2b2cInquiryService.submit(request));
    }

    @GetMapping("/admin/b2b2c-inquiries")
    public ApiResponse<List<B2b2cInquiryItem>> listB2b2cInquiries() {
        return ApiResponse.ok(b2b2cInquiryService.listAll());
    }

    @GetMapping("/org/volume-preview")
    public ApiResponse<OrgVolumePreviewResponse> orgVolumePreview(
            @RequestParam String planType, @RequestParam(defaultValue = "1") int licenseCount) {
        OrgSubscription plan = OrgSubscription.fromPlanType(planType);
        long unitPrice = OrgPlanLimits.forPlan(plan).priceVnd();
        BillingSettingsService.OrgVolumePricing pricing =
                billingSettingsService.calculateOrgVolumePricing(unitPrice, licenseCount);
        return ApiResponse.ok(new OrgVolumePreviewResponse(
                planType.toUpperCase(),
                pricing.licenseCount(),
                unitPrice,
                pricing.subtotalVnd(),
                pricing.discountPercent(),
                pricing.discountAmountVnd(),
                pricing.totalVnd()));
    }

    private UUID requireUserId() {
        return currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
    }
}

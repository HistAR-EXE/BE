package com.histar.be.billing.service;

import com.histar.be.auth.service.EmailVerifiedGuard;
import com.histar.be.billing.OrgPlanLimits;
import com.histar.be.billing.dto.OrgCreatePaymentRequest;
import com.histar.be.billing.service.BillingSettingsService;
import com.histar.be.billing.dto.OrgPaymentIntentResponse;
import com.histar.be.billing.dto.OrgPaymentStatusResponse;
import com.histar.be.billing.dto.OrgSubscribeRequest;
import com.histar.be.billing.dto.SepayWebhookPayload;
import com.histar.be.billing.entity.OrgPaymentTransaction;
import com.histar.be.billing.repository.OrgPaymentTransactionRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.config.SepayProperties;
import com.histar.be.organization.entity.OrgSubscription;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SepayOrgPaymentService {

    private final OrgPaymentTransactionRepository orgPaymentTransactionRepository;
    private final ProfileRepository profileRepository;
    private final OrganizationRepository organizationRepository;
    private final BillingService billingService;
    private final SepayProperties sepayProperties;
    private final BillingSettingsService billingSettingsService;
    private final EmailVerifiedGuard emailVerifiedGuard;

    @Transactional
    public OrgPaymentIntentResponse createPayment(UUID userId, OrgCreatePaymentRequest request) {
        ensureEnabled();
        emailVerifiedGuard.assertEmailVerified(userId);
        Profile profile = profileRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        OrgSubscription plan = OrgSubscription.fromPlanType(request.planType());
        if (plan == OrgSubscription.NONE) {
            throw new BusinessRuleException("planType phải là MICRO, STANDARD hoặc PREMIUM");
        }

        Organization existingOrg = resolveOrganization(profile, request.organizationId());
        String orgName = resolveOrgName(request.orgName(), existingOrg);
        String contactEmail = resolveContactEmail(request.contactEmail(), profile.getEmail(), existingOrg);
        long unitPriceVnd = OrgPlanLimits.forPlan(plan).priceVnd();
        if (unitPriceVnd <= 0) {
            throw new BusinessRuleException("Giá gói B2B không hợp lệ.");
        }
        int licenseCount = request.licenseCount() != null && request.licenseCount() > 0 ? request.licenseCount() : 1;
        BillingSettingsService.OrgVolumePricing pricing =
                billingSettingsService.calculateOrgVolumePricing(unitPriceVnd, licenseCount);
        long amountVnd = pricing.totalVnd();

        Instant now = Instant.now();
        OrgPaymentTransaction pending = orgPaymentTransactionRepository
                .findFirstByRequesterUserIdAndStatusOrderByCreatedAtDesc(userId, "PENDING")
                .filter(tx -> tx.getExpiresAt() != null && tx.getExpiresAt().isAfter(now))
                .orElse(null);
        if (pending != null && pending.getPlanType().equals(plan.name())) {
            return toIntentResponse(pending, pricing);
        }

        String orderCode = generateOrderCode();
        Instant expiresAt = now.plus(sepayProperties.getOrderExpiryMinutes(), ChronoUnit.MINUTES);
        String qrUrl = buildQrUrl(orderCode, amountVnd);
        OrgPaymentTransaction tx = orgPaymentTransactionRepository.save(OrgPaymentTransaction.builder()
                .requesterUserId(userId)
                .organizationId(existingOrg != null ? existingOrg.getId() : null)
                .orgName(orgName)
                .contactEmail(contactEmail)
                .planType(plan.name())
                .provider("SEPAY")
                .orderCode(orderCode)
                .transferContent(orderCode)
                .amountVnd(amountVnd)
                .status("PENDING")
                .returnToPath(request.returnToPath())
                .qrUrl(qrUrl)
                .expiresAt(expiresAt)
                .createdAt(now)
                .updatedAt(now)
                .build());
        return toIntentResponse(tx, pricing);
    }

    @Transactional(readOnly = true)
    public OrgPaymentStatusResponse getPaymentStatus(UUID userId, String orderCode) {
        OrgPaymentTransaction tx = orgPaymentTransactionRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!tx.getRequesterUserId().equals(userId)) {
            throw new BusinessRuleException("Không có quyền xem giao dịch này");
        }
        String status = resolveStatus(tx);
        return new OrgPaymentStatusResponse(
                tx.getOrderCode(),
                status,
                tx.getExpiresAt(),
                tx.getPaidAt(),
                tx.getReturnToPath(),
                "PAID".equals(status),
                tx.getOrganizationId(),
                tx.getPlanType(),
                tx.getOrgName());
    }

    @Transactional
    public void handleWebhook(String timestampHeader, String signatureHeader, String rawBody) {
        ensureEnabled();
        SepayWebhookPayload payload = SepayB2cPaymentService.parseAndVerifyWebhook(
                sepayProperties.getWebhookSecret(),
                sepayProperties.getMaxTimestampSkewSeconds(),
                timestampHeader,
                signatureHeader,
                rawBody);
        handleVerifiedWebhook(payload);
    }

    @Transactional
    void handleVerifiedWebhook(SepayWebhookPayload payload) {
        if (!"in".equalsIgnoreCase(payload.transferType())) {
            return;
        }
        if (orgPaymentTransactionRepository.findByProviderTransactionId(payload.id()).isPresent()) {
            return;
        }
        String code = payload.code() != null && !payload.code().isBlank() ? payload.code().trim() : extractCode(payload.content());
        if (code == null || code.isBlank()) {
            return;
        }
        OrgPaymentTransaction tx = orgPaymentTransactionRepository.findByOrderCode(code)
                .orElseGet(() -> orgPaymentTransactionRepository.findByTransferContent(code).orElse(null));
        if (tx == null) {
            return;
        }
        if ("PAID".equalsIgnoreCase(tx.getStatus())) {
            if (tx.getProviderTransactionId() == null) {
                tx.setProviderTransactionId(payload.id());
                tx.setUpdatedAt(Instant.now());
                orgPaymentTransactionRepository.save(tx);
            }
            return;
        }
        if (payload.transferAmount() == null || payload.transferAmount() < tx.getAmountVnd()) {
            tx.setProviderPayload(payload.content());
            tx.setUpdatedAt(Instant.now());
            orgPaymentTransactionRepository.save(tx);
            return;
        }

        tx.setStatus("PAID");
        tx.setPaidAt(Instant.now());
        tx.setProviderTransactionId(payload.id());
        tx.setProviderReferenceCode(payload.referenceCode());
        tx.setProviderGateway(payload.gateway());
        tx.setProviderPayload(payload.content());
        tx.setUpdatedAt(Instant.now());
        orgPaymentTransactionRepository.save(tx);

        var orgStatus = billingService.subscribeOrg(
                tx.getRequesterUserId(),
                new OrgSubscribeRequest(tx.getOrgName(), tx.getPlanType(), tx.getContactEmail(), tx.getOrganizationId()),
                "SEPAY");
        tx.setOrganizationId(orgStatus.organizationId());
        orgPaymentTransactionRepository.save(tx);
    }

    private void ensureEnabled() {
        if (!sepayProperties.isEnabled()) {
            throw new BusinessRuleException("SePay chưa được cấu hình để sử dụng.");
        }
        if (sepayProperties.getBankCode().isBlank()
                || sepayProperties.getAccountNumber().isBlank()
                || sepayProperties.getAccountName().isBlank()) {
            throw new BusinessRuleException("Thiếu cấu hình tài khoản SePay/VietQR.");
        }
    }

    private Organization resolveOrganization(Profile profile, UUID requestOrganizationId) {
        UUID orgId = requestOrganizationId != null ? requestOrganizationId : profile.getOrgId();
        if (orgId == null) {
            return null;
        }
        return organizationRepository.findById(orgId).orElse(null);
    }

    private String resolveOrgName(String requestName, Organization existingOrg) {
        String value = requestName != null && !requestName.trim().isBlank()
                ? requestName.trim()
                : existingOrg != null ? existingOrg.getName() : "";
        if (value.isBlank()) {
            throw new BusinessRuleException("Tên tổ chức không được rỗng");
        }
        return value;
    }

    private String resolveContactEmail(String requestEmail, String profileEmail, Organization existingOrg) {
        String value = requestEmail != null && !requestEmail.trim().isBlank()
                ? requestEmail.trim()
                : existingOrg != null && existingOrg.getContactEmail() != null ? existingOrg.getContactEmail() : profileEmail;
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Email liên hệ không được rỗng");
        }
        return value;
    }

    private OrgPaymentIntentResponse toIntentResponse(
            OrgPaymentTransaction tx, BillingSettingsService.OrgVolumePricing pricing) {
        return new OrgPaymentIntentResponse(
                tx.getProvider(),
                tx.getOrderCode(),
                tx.getTransferContent(),
                tx.getAmountVnd(),
                pricing.subtotalVnd(),
                pricing.discountPercent(),
                pricing.discountAmountVnd(),
                pricing.licenseCount(),
                sepayProperties.getBankCode(),
                sepayProperties.getAccountNumber(),
                sepayProperties.getAccountName(),
                tx.getQrUrl(),
                tx.getExpiresAt(),
                resolveStatus(tx),
                tx.getPlanType(),
                tx.getOrganizationId(),
                tx.getOrgName());
    }

    private String resolveStatus(OrgPaymentTransaction tx) {
        if ("PAID".equalsIgnoreCase(tx.getStatus())) {
            return "PAID";
        }
        if (tx.getExpiresAt() != null && tx.getExpiresAt().isBefore(Instant.now())) {
            return "EXPIRED";
        }
        return tx.getStatus();
    }

    private String generateOrderCode() {
        return "ORG" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
    }

    private String buildQrUrl(String orderCode, long amount) {
        String base = sepayProperties.getQrBaseUrl().isBlank() ? "https://vietqr.app/img" : sepayProperties.getQrBaseUrl();
        return base
                + "?bank=" + urlEncode(sepayProperties.getBankCode())
                + "&acc=" + urlEncode(sepayProperties.getAccountNumber())
                + "&template=" + urlEncode(sepayProperties.getQrTemplate())
                + "&amount=" + amount
                + "&des=" + urlEncode(orderCode)
                + "&showinfo=" + sepayProperties.isQrShowInfo()
                + "&holder=" + urlEncode(sepayProperties.getAccountName());
    }

    private static String extractCode(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String[] parts = content.trim().split("\\s+");
        return parts.length == 0 ? null : parts[0].trim();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}

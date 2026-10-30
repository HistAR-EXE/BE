package com.histar.be.billing.service;

import com.histar.be.auth.service.EmailVerifiedGuard;
import com.histar.be.billing.policy.OrgPlanLimits;
import com.histar.be.billing.dto.AdminCreateB2bInvoiceRequest;
import com.histar.be.billing.dto.OrgCreatePaymentRequest;
import com.histar.be.billing.service.BillingSettingsService;
import com.histar.be.billing.dto.OrgPaymentIntentResponse;
import com.histar.be.billing.dto.OrgPaymentStatusResponse;
import com.histar.be.billing.dto.OrgSubscribeRequest;
import com.histar.be.billing.dto.SepayWebhookPayload;
import com.histar.be.billing.entity.OrgPaymentTransaction;
import com.histar.be.billing.repository.OrgPaymentTransactionRepository;
import com.histar.be.billing.repository.OrgBillingSubscriptionRepository;
import com.histar.be.billing.entity.OrgBillingSubscription;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SepayOrgPaymentService {

    static final String STATUS_UNDERPAID = "UNDERPAID";

    private final OrgPaymentTransactionRepository orgPaymentTransactionRepository;
    private final ProfileRepository profileRepository;
    private final OrganizationRepository organizationRepository;
    private final BillingService billingService;
    private final OrgPaidActivationService paidActivation;
    private final SepayProperties sepayProperties;
    private final BillingSettingsService billingSettingsService;
    private final EmailVerifiedGuard emailVerifiedGuard;
    private final OrgBillingSubscriptionRepository orgBillingSubscriptionRepository;
    private final com.histar.be.organization.repository.OrganizationMemberRepository organizationMemberRepository;

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
        if (existingOrg != null) {
            requireBillingOwner(profile, existingOrg.getId());
            organizationRepository.findByIdForUpdate(existingOrg.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
            com.histar.be.billing.policy.OrgSeatCapacityPolicy.requireFits(
                    organizationMemberRepository.countByOrganizationId(existingOrg.getId()), plan);
        }
        UUID requestedOrgId = existingOrg == null ? null : existingOrg.getId();
        String orgName = resolveOrgName(request.orgName(), existingOrg);
        String contactEmail = resolveContactEmail(request.contactEmail(), profile.getEmail(), existingOrg);
        long unitPriceVnd = OrgPlanLimits.forPlan(plan).priceVnd();
        if (unitPriceVnd <= 0) {
            throw new BusinessRuleException("Giá gói B2B không hợp lệ.");
        }
        int licenseCount = com.histar.be.billing.policy.OrgSeatCapacityPolicy.requireSingleLicense(request.licenseCount());
        BillingSettingsService.OrgVolumePricing pricing =
                billingSettingsService.calculateOrgVolumePricing(unitPriceVnd, licenseCount);
        long amountVnd = pricing.totalVnd();

        Instant now = Instant.now();
        OrgPaymentTransaction underpaid = orgPaymentTransactionRepository
                .findFirstByRequesterUserIdAndStatusOrderByCreatedAtDesc(userId, STATUS_UNDERPAID)
                .filter(tx -> tx.getExpiresAt() != null
                        && !SepayB2cPaymentService.isPastGrace(
                                tx.getExpiresAt(), sepayProperties.getExpiryGraceMinutes()))
                .filter(tx -> tx.getPlanType().equals(plan.name()))
                .filter(tx -> matchesRequestedOrganization(tx, requestedOrgId, orgName, contactEmail))
                .orElse(null);
        if (underpaid != null) {
            return reuseIntent(underpaid, pricing);
        }
        OrgPaymentTransaction pending = orgPaymentTransactionRepository
                .findFirstByRequesterUserIdAndStatusOrderByCreatedAtDesc(userId, "PENDING")
                .filter(tx -> tx.getExpiresAt() != null && tx.getExpiresAt().isAfter(now))
                .orElse(null);
        if (pending != null && pending.getPlanType().equals(plan.name())
                && matchesRequestedOrganization(pending, requestedOrgId, orgName, contactEmail)) {
            return reuseIntent(pending, pricing);
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
                .licenseCount(pricing.licenseCount())
                .grossAmountVnd(pricing.subtotalVnd())
                .discountAmountVnd(pricing.discountAmountVnd())
                .status("PENDING")
                .returnToPath(request.returnToPath())
                .qrUrl(qrUrl)
                .expiresAt(expiresAt)
                .createdAt(now)
                .updatedAt(now)
                .build());
        return toIntentResponse(tx);
    }

    /**
     * Admin-issued B2B invoice — always creates a fresh PENDING intent (no reuse / email verify).
     * When SePay is configured, QR uses VietQR; otherwise provider=ADMIN and qrUrl is empty.
     */
    @Transactional
    public OrgPaymentIntentResponse createAdminInvoice(UUID adminUserId, AdminCreateB2bInvoiceRequest request) {
        profileRepository.findById(adminUserId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        OrgSubscription plan = OrgSubscription.fromPlanType(request.planType());
        if (plan == OrgSubscription.NONE) {
            throw new BusinessRuleException("planType phải là MICRO, STANDARD hoặc PREMIUM");
        }

        Organization existingOrg = null;
        if (request.organizationId() != null) {
            existingOrg = organizationRepository
                    .findById(request.organizationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Organization not found: " + request.organizationId()));
        }
        if (existingOrg != null) {
            organizationRepository.findByIdForUpdate(existingOrg.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
            com.histar.be.billing.policy.OrgSeatCapacityPolicy.requireFits(
                    organizationMemberRepository.countByOrganizationId(existingOrg.getId()), plan);
        }
        String orgName = resolveOrgName(request.orgName(), existingOrg);
        String contactEmail = resolveContactEmail(request.contactEmail(), null, existingOrg);
        long unitPriceVnd = OrgPlanLimits.forPlan(plan).priceVnd();
        if (unitPriceVnd <= 0) {
            throw new BusinessRuleException("Giá gói B2B không hợp lệ.");
        }
        int licenseCount = com.histar.be.billing.policy.OrgSeatCapacityPolicy.requireSingleLicense(request.licenseCount());
        BillingSettingsService.OrgVolumePricing pricing =
                billingSettingsService.calculateOrgVolumePricing(unitPriceVnd, licenseCount);
        long amountVnd = pricing.totalVnd();

        boolean sepayReady = sepayProperties.isEnabled()
                && !sepayProperties.getBankCode().isBlank()
                && !sepayProperties.getAccountNumber().isBlank()
                && !sepayProperties.getAccountName().isBlank();

        Instant now = Instant.now();
        String orderCode = generateOrderCode();
        Instant expiresAt = now.plus(
                Math.max(15, sepayProperties.getOrderExpiryMinutes()), ChronoUnit.MINUTES);
        String qrUrl = sepayReady ? buildQrUrl(orderCode, amountVnd) : "";
        OrgPaymentTransaction tx = orgPaymentTransactionRepository.save(OrgPaymentTransaction.builder()
                .requesterUserId(adminUserId)
                .organizationId(existingOrg != null ? existingOrg.getId() : null)
                .orgName(orgName)
                .contactEmail(contactEmail)
                .planType(plan.name())
                .provider(sepayReady ? "SEPAY" : "ADMIN")
                .orderCode(orderCode)
                .transferContent(orderCode)
                .amountVnd(amountVnd)
                .licenseCount(pricing.licenseCount())
                .grossAmountVnd(pricing.subtotalVnd())
                .discountAmountVnd(pricing.discountAmountVnd())
                .status("PENDING")
                .returnToPath(null)
                .qrUrl(qrUrl)
                .expiresAt(expiresAt)
                .createdAt(now)
                .updatedAt(now)
                .build());
        return toIntentResponse(tx);
    }

    @Transactional
    public OrgPaymentStatusResponse getPaymentStatus(UUID userId, String orderCode) {
        OrgPaymentTransaction tx = orgPaymentTransactionRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!tx.getRequesterUserId().equals(userId)) {
            throw new BusinessRuleException("Không có quyền xem giao dịch này");
        }
        String status = resolveStatus(tx);
        if ("PAID".equals(status) && tx.getSubscriptionId() == null) finalizePaidOrg(tx);
        int received = STATUS_UNDERPAID.equals(status)
                ? SepayB2cPaymentService.receivedAmountFromPayload(tx.getProviderPayload())
                : 0;
        int amount = tx.getAmountVnd() != null ? tx.getAmountVnd().intValue() : 0;
        int remaining = Math.max(0, amount - received);
        return new OrgPaymentStatusResponse(
                tx.getOrderCode(),
                status,
                tx.getExpiresAt(),
                tx.getPaidAt(),
                tx.getReturnToPath(),
                "PAID".equals(status) && tx.getSubscriptionId() != null,
                tx.getOrganizationId(),
                tx.getPlanType(),
                tx.getOrgName(),
                amount,
                tx.getTransferContent(),
                STATUS_UNDERPAID.equals(status) ? received : null,
                STATUS_UNDERPAID.equals(status) ? remaining : null,
                "PAID".equals(status) && tx.getSubscriptionId() == null ? activationMessage(tx) : null);
    }

    @Transactional
    public void handleWebhook(String timestampHeader, String signatureHeader, String rawBody) {
        handleWebhook(timestampHeader, signatureHeader, null, rawBody);
    }

    @Transactional
    public void handleWebhook(
            String timestampHeader, String signatureHeader, String authorizationHeader, String rawBody) {
        ensureEnabled();
        SepayWebhookPayload payload = SepayB2cPaymentService.authenticateAndParse(
                sepayProperties, timestampHeader, signatureHeader, authorizationHeader, rawBody);
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
        OrgPaymentTransaction tx = orgPaymentTransactionRepository.findByOrderCodeForUpdate(code)
                .orElseGet(() -> orgPaymentTransactionRepository.findByTransferContentForUpdate(code).orElse(null));
        if (tx == null) {
            log.info("SePay B2B webhook ignored: no order for code={}", code);
            return;
        }
        if ("REFUND".equalsIgnoreCase(tx.getStatus())
                || "REFUNDED".equalsIgnoreCase(tx.getStatus())
                || "VOID".equalsIgnoreCase(tx.getStatus())
                || "VOIDED".equalsIgnoreCase(tx.getStatus())) {
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
        long thisTransfer = payload.transferAmount() == null ? 0L : payload.transferAmount();
        long previouslyReceived = STATUS_UNDERPAID.equalsIgnoreCase(tx.getStatus())
                ? SepayB2cPaymentService.receivedAmountFromPayload(tx.getProviderPayload())
                : 0L;
        long cumulative = previouslyReceived + thisTransfer;
        if (cumulative < tx.getAmountVnd()) {
            log.warn(
                    "SePay B2B webhook underpaid order={} expected={} got={} cumulative={}",
                    tx.getOrderCode(),
                    tx.getAmountVnd(),
                    payload.transferAmount(),
                    cumulative);
            tx.setStatus(STATUS_UNDERPAID);
            tx.setProviderTransactionId(payload.id());
            tx.setProviderPayload(toReceivedJson(payload, cumulative));
            tx.setUpdatedAt(Instant.now());
            orgPaymentTransactionRepository.save(tx);
            return;
        }

        tx.setStatus("PAID");
        tx.setPaidAt(Instant.now());
        tx.setProviderTransactionId(payload.id());
        tx.setProviderReferenceCode(payload.referenceCode());
        tx.setProviderGateway(payload.gateway());
        tx.setProviderPayload(toReceivedJson(payload, cumulative));
        tx.setUpdatedAt(Instant.now());
        orgPaymentTransactionRepository.save(tx);
        finalizePaidOrg(tx);
    }

    /**
     * Activate org subscription after a transaction is already marked PAID.
     * Shared by SePay webhook and admin mark-paid.
     */
    @Transactional
    public void finalizePaidOrg(OrgPaymentTransaction tx) {
        if (tx.getSubscriptionId() != null) return;
        UUID id = tx.getId();
        Runnable attempt = () -> {
            try { paidActivation.activate(id); }
            catch (RuntimeException failure) {
                // The receipt transaction has committed. Activation rollback must never erase money.
                log.warn("B2B payment {} awaits activation ({})", id, failure.getClass().getSimpleName());
            }
        };
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCommit() { attempt.run(); }
                    });
        } else attempt.run();
    }

    void activatePaidOrgOnce(OrgPaymentTransaction tx) {
        if (tx.getSubscriptionId() != null) {
            return;
        }
        // Payment -> account -> organization matches all activation paths and avoids
        // reversing enrollment's account -> organization lock order.
        profileRepository.findByIdForUpdate(tx.getRequesterUserId());
        if (capacityMessage(tx) != null) return;
        var orgStatus = billingService.subscribeOrg(
                tx.getRequesterUserId(),
                new OrgSubscribeRequest(tx.getOrgName(), tx.getPlanType(), tx.getContactEmail(), tx.getOrganizationId()),
                "SEPAY");
        tx.setOrganizationId(orgStatus.organizationId());
        OrgBillingSubscription subscription = orgBillingSubscriptionRepository
                .findAllByOrganizationIdOrderByCreatedAtDesc(orgStatus.organizationId()).stream()
                .filter(sub -> Boolean.TRUE.equals(sub.getIsActive()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy subscription vừa kích hoạt"));
        subscription.setSourcePaymentTransactionId(tx.getId());
        subscription.setBillingCadence("ANNUAL");
        subscription.setContractValueVnd(tx.getAmountVnd());
        subscription.setCurrency("VND");
        subscription.setQuantity(tx.getLicenseCount());
        orgBillingSubscriptionRepository.save(subscription);
        tx.setSubscriptionId(subscription.getId());
        orgPaymentTransactionRepository.save(tx);
    }

    private String activationMessage(OrgPaymentTransaction tx) {
        String capacity = null;
        try { capacity = capacityMessage(tx); }
        catch (ResourceNotFoundException missingOrganization) {
            // Preserve access to the received-payment status even if the organization was removed.
        }
        return capacity != null ? capacity : "Đã nhận thanh toán nhưng chưa hoàn tất điều kiện kích hoạt. Hãy kiểm tra quyền chủ tổ chức và email hoặc liên hệ hỗ trợ. Không chuyển khoản thêm.";
    }

    private String capacityMessage(OrgPaymentTransaction tx) {
        if (tx.getOrganizationId() == null) return null;
        organizationRepository.findByIdForUpdate(tx.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        try {
            com.histar.be.billing.policy.OrgSeatCapacityPolicy.requireFits(
                    organizationMemberRepository.countByOrganizationId(tx.getOrganizationId()),
                    OrgSubscription.fromPlanType(tx.getPlanType()));
            return null;
        } catch (com.histar.be.billing.policy.OrgSeatCapacityPolicy.CapacityExceeded ex) {
            return ex.getMessage();
        }
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
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }

    private void requireBillingOwner(Profile profile, UUID orgId) {
        if (com.histar.be.profile.entity.UserRole.ADMIN == com.histar.be.profile.entity.UserRole.fromStored(profile.getRole())) return;
        boolean owner = organizationMemberRepository.findByOrganizationIdAndUserId(orgId, profile.getId())
                .filter(member -> member.getAcceptedAt() != null && "owner".equalsIgnoreCase(member.getOrgRole())).isPresent();
        // An expired organization must still be renewable by its accepted owner.
        if (!owner) throw new BusinessRuleException("Chỉ chủ tổ chức mới được thay đổi gói B2B");
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

    private static boolean matchesRequestedOrganization(OrgPaymentTransaction payment, UUID organizationId,
            String name, String email) {
        return java.util.Objects.equals(payment.getOrganizationId(), organizationId)
                && java.util.Objects.equals(payment.getOrgName(), name)
                && java.util.Objects.equals(payment.getContactEmail(), email);
    }

    private OrgPaymentIntentResponse reuseIntent(
            OrgPaymentTransaction tx, BillingSettingsService.OrgVolumePricing requested) {
        if (tx.getLicenseCount() == null || tx.getGrossAmountVnd() == null || tx.getDiscountAmountVnd() == null) {
            throw new BusinessRuleException("Payment intent cũ thiếu pricing snapshot; vui lòng tạo intent mới");
        }
        if (tx.getLicenseCount() != requested.licenseCount()
                || tx.getGrossAmountVnd() != requested.subtotalVnd()
                || tx.getDiscountAmountVnd() != requested.discountAmountVnd()
                || tx.getAmountVnd() != requested.totalVnd()) {
            throw new BusinessRuleException("Pricing hiện tại không khớp payment intent đang chờ");
        }
        return toIntentResponse(tx);
    }

    private OrgPaymentIntentResponse toIntentResponse(OrgPaymentTransaction tx) {
        long gross = tx.getGrossAmountVnd();
        int discountPercent = gross == 0 ? 0 : (int) Math.round(tx.getDiscountAmountVnd() * 100.0 / gross);
        return new OrgPaymentIntentResponse(
                tx.getProvider(),
                tx.getOrderCode(),
                tx.getTransferContent(),
                tx.getAmountVnd(),
                tx.getGrossAmountVnd(),
                discountPercent,
                tx.getDiscountAmountVnd(),
                tx.getLicenseCount(),
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
        if (STATUS_UNDERPAID.equalsIgnoreCase(tx.getStatus())) {
            if (SepayB2cPaymentService.isPastGrace(tx.getExpiresAt(), sepayProperties.getExpiryGraceMinutes())) {
                return "EXPIRED";
            }
            return STATUS_UNDERPAID;
        }
        if (SepayB2cPaymentService.isPastGrace(tx.getExpiresAt(), sepayProperties.getExpiryGraceMinutes())) {
            return "EXPIRED";
        }
        return tx.getStatus();
    }

    private static String toReceivedJson(SepayWebhookPayload payload, long receivedAmount) {
        String content = payload.content() == null ? "" : payload.content().replace("\"", "\\\"");
        return "{\"id\":"
                + payload.id()
                + ",\"receivedAmount\":"
                + receivedAmount
                + ",\"content\":\""
                + content
                + "\"}";
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

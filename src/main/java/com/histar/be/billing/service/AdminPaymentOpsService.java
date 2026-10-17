package com.histar.be.billing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.billing.dto.AdminPaymentOperationResponse;
import com.histar.be.billing.entity.B2cPaymentTransaction;
import com.histar.be.billing.entity.B2cSubscription;
import com.histar.be.billing.entity.OrgBillingSubscription;
import com.histar.be.billing.entity.OrgPaymentTransaction;
import com.histar.be.billing.repository.B2cPaymentTransactionRepository;
import com.histar.be.billing.repository.B2cSubscriptionRepository;
import com.histar.be.billing.repository.B2cVisitEntitlementRepository;
import com.histar.be.billing.repository.OrgBillingSubscriptionRepository;
import com.histar.be.billing.repository.OrgPaymentTransactionRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPaymentOpsService {
    private static final Set<String> PAYABLE_STATUSES = Set.of("PENDING", "UNDERPAID");
    private static final String B2C_ENTITY = "B2C_PAYMENT";
    private static final String B2B_ENTITY = "ORG_PAYMENT";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final B2cPaymentTransactionRepository b2cPayments;
    private final OrgPaymentTransactionRepository b2bPayments;
    private final SepayB2cPaymentService b2cFinalizer;
    private final SepayOrgPaymentService b2bFinalizer;
    private final B2cSubscriptionRepository b2cSubscriptions;
    private final B2cVisitEntitlementRepository visitEntitlements;
    private final OrgBillingSubscriptionRepository orgSubscriptions;
    private final ProfileRepository profiles;
    private final OrganizationRepository organizations;
    private final AdminAuditService audit;
    private final CurrentUserAccessor currentUser;

    @Transactional
    public AdminPaymentOperationResponse markPaid(String channel, UUID paymentId, String reason) {
        String normalizedReason = requireReason(reason);
        String normalizedChannel = requireChannel(channel);
        Instant now = Instant.now();
        if ("B2C".equals(normalizedChannel)) {
            B2cPaymentTransaction payment = findB2c(paymentId);
            requireTransition(payment.getStatus(), PAYABLE_STATUSES, "PAID");
            String oldStatus = normalizeStatus(payment.getStatus());
            payment.setStatus("PAID");
            payment.setPaidAt(now);
            payment.setProviderPayload(withReceivedAmount(payment.getProviderPayload(), payment.getAmountVnd()));
            payment.setUpdatedAt(now);
            b2cPayments.save(payment);
            b2cFinalizer.finalizePaidB2c(payment, false);
            appendAudit(payment.getId(), B2C_ENTITY, payment.getOrderCode(), oldStatus, "PAID",
                    "PAYMENT_MARKED_PAID", normalizedReason, normalizedChannel);
            return response(payment, normalizedChannel);
        }
        OrgPaymentTransaction payment = findB2b(paymentId);
        requireTransition(payment.getStatus(), PAYABLE_STATUSES, "PAID");
        String oldStatus = normalizeStatus(payment.getStatus());
        payment.setStatus("PAID");
        payment.setPaidAt(now);
        payment.setProviderPayload(withReceivedAmount(payment.getProviderPayload(), payment.getAmountVnd()));
        payment.setUpdatedAt(now);
        b2bPayments.save(payment);
        b2bFinalizer.finalizePaidOrg(payment);
        appendAudit(payment.getId(), B2B_ENTITY, payment.getOrderCode(), oldStatus, "PAID",
                "PAYMENT_MARKED_PAID", normalizedReason, normalizedChannel);
        return response(payment, normalizedChannel);
    }

    @Transactional
    public AdminPaymentOperationResponse refund(String channel, UUID paymentId, String reason) {
        String normalizedReason = requireReason(reason);
        String normalizedChannel = requireChannel(channel);
        Instant now = Instant.now();
        if ("B2C".equals(normalizedChannel)) {
            B2cPaymentTransaction payment = findB2c(paymentId);
            requireTransition(payment.getStatus(), Set.of("PAID"), "REFUND");
            validateB2cRevoke(payment);
            payment.setStatus("REFUND");
            payment.setUpdatedAt(now);
            b2cPayments.save(payment);
            appendAudit(payment.getId(), B2C_ENTITY, payment.getOrderCode(), "PAID", "REFUND",
                    "PAYMENT_REFUNDED", normalizedReason, normalizedChannel);
            return response(payment, normalizedChannel);
        }
        OrgPaymentTransaction payment = findB2b(paymentId);
        requireTransition(payment.getStatus(), Set.of("PAID"), "REFUND");
        validateB2bRevoke(payment);
        payment.setStatus("REFUND");
        payment.setUpdatedAt(now);
        b2bPayments.save(payment);
        appendAudit(payment.getId(), B2B_ENTITY, payment.getOrderCode(), "PAID", "REFUND",
                "PAYMENT_REFUNDED", normalizedReason, normalizedChannel);
        return response(payment, normalizedChannel);
    }

    @Transactional
    public AdminPaymentOperationResponse voidPayment(String channel, UUID paymentId, String reason) {
        String normalizedReason = requireReason(reason);
        String normalizedChannel = requireChannel(channel);
        Instant now = Instant.now();
        if ("B2C".equals(normalizedChannel)) {
            B2cPaymentTransaction payment = findB2c(paymentId);
            requireTransition(payment.getStatus(), PAYABLE_STATUSES, "VOID");
            String oldStatus = normalizeStatus(payment.getStatus());
            payment.setStatus("VOID");
            payment.setUpdatedAt(now);
            b2cPayments.save(payment);
            appendAudit(payment.getId(), B2C_ENTITY, payment.getOrderCode(), oldStatus, "VOID",
                    "PAYMENT_VOIDED", normalizedReason, normalizedChannel);
            return response(payment, normalizedChannel);
        }
        OrgPaymentTransaction payment = findB2b(paymentId);
        requireTransition(payment.getStatus(), PAYABLE_STATUSES, "VOID");
        String oldStatus = normalizeStatus(payment.getStatus());
        payment.setStatus("VOID");
        payment.setUpdatedAt(now);
        b2bPayments.save(payment);
        appendAudit(payment.getId(), B2B_ENTITY, payment.getOrderCode(), oldStatus, "VOID",
                "PAYMENT_VOIDED", normalizedReason, normalizedChannel);
        return response(payment, normalizedChannel);
    }

    private void validateB2cRevoke(B2cPaymentTransaction payment) {
        if (SepayB2cPaymentService.PLAN_JOURNEY_PASS.equalsIgnoreCase(payment.getPlanType())) {
            var entitlements = visitEntitlements.findByOrderCode(payment.getOrderCode());
            if (entitlements.isEmpty() || entitlements.stream().anyMatch(e ->
                    !payment.getUserId().equals(e.getUserId())
                            || e.getExpiresAt() == null
                            || !e.getExpiresAt().isAfter(Instant.now()))) {
                throw new BusinessRuleException("Linked entitlement is missing, inactive, or owned by another user");
            }
            visitEntitlements.deleteAll(entitlements);
            return;
        }
        if (payment.getSubscriptionId() == null) {
            throw new BusinessRuleException("Linked subscription is missing");
        }
        B2cSubscription linked = b2cSubscriptions.findById(payment.getSubscriptionId()).orElse(null);
        if (linked == null || !Boolean.TRUE.equals(linked.getIsActive())
                || !payment.getUserId().equals(linked.getUserId())) {
            throw new BusinessRuleException("Linked subscription is missing, inactive, or owned by another user");
        }
        linked.setIsActive(false);
        b2cSubscriptions.save(linked);
        boolean anotherActive = b2cSubscriptions.findAllByUserIdOrderByCreatedAtDesc(payment.getUserId()).stream()
                .anyMatch(subscription -> Boolean.TRUE.equals(subscription.getIsActive()));
        if (!anotherActive) {
            profiles.findById(payment.getUserId()).ifPresent(profile -> {
                if (profile.getOrgId() == null) {
                    profile.setTier(UserTier.FREE.name());
                    profiles.save(profile);
                }
            });
        }
    }

    private void validateB2bRevoke(OrgPaymentTransaction payment) {
        if (payment.getSubscriptionId() == null) {
            throw new BusinessRuleException("Linked subscription is missing");
        }
        OrgBillingSubscription linked = orgSubscriptions.findById(payment.getSubscriptionId()).orElse(null);
        if (linked == null || !Boolean.TRUE.equals(linked.getIsActive())
                || payment.getOrganizationId() == null
                || !payment.getOrganizationId().equals(linked.getOrganizationId())) {
            throw new BusinessRuleException("Linked subscription is missing, inactive, or owned by another organization");
        }
        linked.setIsActive(false);
        orgSubscriptions.save(linked);
        UUID organizationId = payment.getOrganizationId() != null
                ? payment.getOrganizationId()
                : linked.getOrganizationId();
        if (organizationId == null) {
            return;
        }
        boolean anotherActive = orgSubscriptions.findAllByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .anyMatch(subscription -> Boolean.TRUE.equals(subscription.getIsActive()));
        if (!anotherActive) {
            organizations.findById(organizationId).ifPresent(organization -> {
                organization.setStatus(BillingService.ORG_STATUS_EXPIRED);
                organizations.save(organization);
            });
        }
    }

    private void appendAudit(
            UUID paymentId,
            String entityType,
            String orderCode,
            String oldStatus,
            String newStatus,
            String action,
            String reason,
            String channel) {
        UUID actorId = currentUser.getUserId()
                .orElseThrow(() -> new BusinessRuleException("Không xác định được quản trị viên"));
        audit.append(new AdminAuditCommand(
                actorId,
                entityType,
                paymentId,
                action,
                null,
                null,
                reason,
                paymentId + ":" + action,
                Map.of(
                        "channel", channel,
                        "orderCode", orderCode,
                        "oldStatus", oldStatus,
                        "newStatus", newStatus)));
    }

    private B2cPaymentTransaction findB2c(UUID paymentId) {
        return b2cPayments.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("B2C payment not found: " + paymentId));
    }

    private OrgPaymentTransaction findB2b(UUID paymentId) {
        return b2bPayments.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("B2B payment not found: " + paymentId));
    }

    private void requireTransition(String currentStatus, Set<String> allowed, String targetStatus) {
        String normalized = normalizeStatus(currentStatus);
        if (!allowed.contains(normalized)) {
            throw new BusinessRuleException("Không thể chuyển payment từ " + normalized + " sang " + targetStatus);
        }
    }

    private String requireReason(String reason) {
        String normalized = reason == null ? "" : reason.trim();
        if (normalized.length() < 10) {
            throw new BusinessRuleException("Lý do cần ít nhất 10 ký tự");
        }
        return normalized;
    }

    private String requireChannel(String channel) {
        String normalized = channel == null ? "" : channel.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("B2C", "B2B").contains(normalized)) {
            throw new BusinessRuleException("channel phải là B2C hoặc B2B");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private String withReceivedAmount(String providerPayload, long receivedAmount) {
        try {
            JsonNode parsed = providerPayload == null || providerPayload.isBlank()
                    ? OBJECT_MAPPER.createObjectNode()
                    : OBJECT_MAPPER.readTree(providerPayload);
            ObjectNode object = parsed != null && parsed.isObject()
                    ? (ObjectNode) parsed
                    : OBJECT_MAPPER.createObjectNode();
            object.put("receivedAmount", receivedAmount);
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (Exception ignored) {
            return "{\"receivedAmount\":" + receivedAmount + "}";
        }
    }

    private AdminPaymentOperationResponse response(B2cPaymentTransaction payment, String channel) {
        return new AdminPaymentOperationResponse(
                payment.getId(), channel, payment.getOrderCode(), payment.getStatus(),
                payment.getPaidAt(), payment.getUpdatedAt());
    }

    private AdminPaymentOperationResponse response(OrgPaymentTransaction payment, String channel) {
        return new AdminPaymentOperationResponse(
                payment.getId(), channel, payment.getOrderCode(), payment.getStatus(),
                payment.getPaidAt(), payment.getUpdatedAt());
    }
}

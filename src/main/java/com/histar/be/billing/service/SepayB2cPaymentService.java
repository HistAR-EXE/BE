package com.histar.be.billing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.billing.dto.B2cPaymentIntentResponse;
import com.histar.be.billing.dto.B2cPaymentStatusResponse;
import com.histar.be.billing.dto.SepayWebhookPayload;
import com.histar.be.billing.entity.B2cPaymentTransaction;
import com.histar.be.billing.repository.B2cPaymentTransactionRepository;
import com.histar.be.auth.service.EmailVerifiedGuard;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.config.SepayProperties;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.repository.ProfileRepository;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SepayB2cPaymentService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final B2cPaymentTransactionRepository paymentTransactionRepository;
    private final ProfileRepository profileRepository;
    private final BillingService billingService;
    private final BillingSettingsService billingSettingsService;
    private final SepayProperties sepayProperties;
    private final EmailVerifiedGuard emailVerifiedGuard;
    @Transactional
    public B2cPaymentIntentResponse createPayment(UUID userId, String returnToPath) {
        ensureEnabled();
        emailVerifiedGuard.assertEmailVerified(userId);
        Profile profile = profileRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (profile.getOrgId() != null) {
            throw new BusinessRuleException("Bạn thuộc tổ chức — không thể thanh toán gói cá nhân B2C.");
        }
        if (UserTier.PREMIUM == UserTier.fromStored(profile.getTier())) {
            throw new BusinessRuleException("Tài khoản đang là Premium.");
        }
        int premiumPriceVnd = billingSettingsService.getB2cPremiumPriceVnd();
        if (premiumPriceVnd <= 0) {
            throw new BusinessRuleException("Giá gói Premium B2C chưa được cấu hình hợp lệ.");
        }

        Instant now = Instant.now();
        B2cPaymentTransaction pending = paymentTransactionRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, "PENDING")
                .filter(tx -> tx.getExpiresAt() != null && tx.getExpiresAt().isAfter(now))
                .orElse(null);
        if (pending != null) {
            return toIntentResponse(pending);
        }

        String orderCode = generateOrderCode();
        Instant expiresAt = now.plus(sepayProperties.getOrderExpiryMinutes(), ChronoUnit.MINUTES);
        String qrUrl = buildQrUrl(orderCode, premiumPriceVnd);
        B2cPaymentTransaction tx = paymentTransactionRepository.save(B2cPaymentTransaction.builder()
                .userId(userId)
                .provider("SEPAY")
                .orderCode(orderCode)
                .transferContent(orderCode)
                .amountVnd(premiumPriceVnd)
                .status("PENDING")
                .returnToPath(returnToPath)
                .qrUrl(qrUrl)
                .expiresAt(expiresAt)
                .createdAt(now)
                .updatedAt(now)
                .build());
        return toIntentResponse(tx);
    }

    @Transactional(readOnly = true)
    public B2cPaymentStatusResponse getPaymentStatus(UUID userId, String orderCode) {
        B2cPaymentTransaction tx = paymentTransactionRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!tx.getUserId().equals(userId)) {
            throw new BusinessRuleException("Không có quyền xem giao dịch này");
        }
        String status = resolveStatus(tx);
        return new B2cPaymentStatusResponse(
                tx.getOrderCode(),
                status,
                tx.getExpiresAt(),
                tx.getPaidAt(),
                tx.getReturnToPath(),
                "PAID".equals(status));
    }

    @Transactional
    public void handleWebhook(String timestampHeader, String signatureHeader, String rawBody) {
        handleWebhook(timestampHeader, signatureHeader, null, rawBody);
    }

    @Transactional
    public void handleWebhook(
            String timestampHeader, String signatureHeader, String authorizationHeader, String rawBody) {
        ensureEnabled();
        SepayWebhookPayload payload = authenticateAndParse(
                sepayProperties, timestampHeader, signatureHeader, authorizationHeader, rawBody);
        handleVerifiedWebhook(payload);
    }

    @Transactional
    void handleVerifiedWebhook(SepayWebhookPayload payload) {
        if (!"in".equalsIgnoreCase(payload.transferType())) {
            log.info("SePay B2C webhook ignored: transferType={}", payload.transferType());
            return;
        }
        if (paymentTransactionRepository.findByProviderTransactionId(payload.id()).isPresent()) {
            return;
        }
        String code = payload.code() != null && !payload.code().isBlank() ? payload.code().trim() : extractCode(payload.content());
        if (code == null || code.isBlank()) {
            log.info("SePay B2C webhook ignored: no order code in payload id={}", payload.id());
            return;
        }
        B2cPaymentTransaction tx = paymentTransactionRepository.findByOrderCode(code)
                .orElseGet(() -> paymentTransactionRepository.findByTransferContent(code).orElse(null));
        if (tx == null) {
            log.info("SePay B2C webhook ignored: no order for code={}", code);
            return;
        }
        if ("PAID".equalsIgnoreCase(tx.getStatus())) {
            if (tx.getProviderTransactionId() == null) {
                tx.setProviderTransactionId(payload.id());
                tx.setUpdatedAt(Instant.now());
                paymentTransactionRepository.save(tx);
            }
            return;
        }
        if (payload.transferAmount() == null || payload.transferAmount() < tx.getAmountVnd()) {
            log.warn(
                    "SePay B2C webhook underpaid order={} expected={} got={}",
                    tx.getOrderCode(),
                    tx.getAmountVnd(),
                    payload.transferAmount());
            tx.setProviderPayload(toJson(payload));
            tx.setUpdatedAt(Instant.now());
            paymentTransactionRepository.save(tx);
            return;
        }
        tx.setStatus("PAID");
        tx.setPaidAt(Instant.now());
        tx.setProviderTransactionId(payload.id());
        tx.setProviderReferenceCode(payload.referenceCode());
        tx.setProviderGateway(payload.gateway());
        tx.setProviderPayload(toJson(payload));
        tx.setUpdatedAt(Instant.now());
        paymentTransactionRepository.save(tx);
        billingService.subscribeB2c(tx.getUserId(), "SEPAY");
        log.info("SePay B2C webhook matched order={} status=PAID", tx.getOrderCode());
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

    static SepayWebhookPayload parseAndVerifyWebhook(
            String secret,
            long maxTimestampSkewSeconds,
            String timestampHeader,
            String signatureHeader,
            String rawBody) {
        if (secret == null || secret.isBlank()) {
            throw new BusinessRuleException("Thiếu cấu hình webhook secret của SePay.");
        }
        if (timestampHeader == null || timestampHeader.isBlank() || signatureHeader == null || signatureHeader.isBlank()) {
            throw new BusinessRuleException("Webhook SePay không hợp lệ.");
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader.trim());
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("Timestamp webhook SePay không hợp lệ.");
        }

        long now = Instant.now().getEpochSecond();
        long skew = Math.abs(now - timestamp);
        if (skew > maxTimestampSkewSeconds) {
            throw new BusinessRuleException("Timestamp webhook SePay đã hết hạn.");
        }

        String expectedSignature = "sha256=" + hmacSha256Hex(secret, timestampHeader.trim() + "." + (rawBody == null ? "" : rawBody));
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signatureHeader.trim().getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessRuleException("Chữ ký webhook SePay không hợp lệ.");
        }
        if (rawBody == null || rawBody.isBlank()) {
            throw new BusinessRuleException("Webhook payload trống.");
        }
        try {
            return OBJECT_MAPPER.readValue(rawBody, SepayWebhookPayload.class);
        } catch (JsonProcessingException e) {
            throw new BusinessRuleException("Webhook payload không phải JSON hợp lệ.");
        }
    }

    private B2cPaymentIntentResponse toIntentResponse(B2cPaymentTransaction tx) {
        return new B2cPaymentIntentResponse(
                tx.getProvider(),
                tx.getOrderCode(),
                tx.getTransferContent(),
                tx.getAmountVnd(),
                sepayProperties.getBankCode(),
                sepayProperties.getAccountNumber(),
                sepayProperties.getAccountName(),
                tx.getQrUrl(),
                tx.getExpiresAt(),
                resolveStatus(tx));
    }

    private String resolveStatus(B2cPaymentTransaction tx) {
        if ("PAID".equalsIgnoreCase(tx.getStatus())) {
            return "PAID";
        }
        if (isPastGrace(tx.getExpiresAt(), sepayProperties.getExpiryGraceMinutes())) {
            return "EXPIRED";
        }
        return tx.getStatus();
    }

    static boolean isPastGrace(Instant expiresAt, int graceMinutes) {
        if (expiresAt == null) {
            return false;
        }
        int grace = Math.max(0, graceMinutes);
        return !expiresAt.plus(grace, ChronoUnit.MINUTES).isAfter(Instant.now());
    }

    /**
     * SePay production sends {@code Authorization: Apikey <key>}. Scripts and unit tests still use HMAC
     * headers. Mode {@code auto} accepts either; {@code apikey} and {@code hmac} force one scheme.
     */
    static SepayWebhookPayload authenticateAndParse(
            SepayProperties properties,
            String timestampHeader,
            String signatureHeader,
            String authorizationHeader,
            String rawBody) {
        String mode = properties.getWebhookAuthMode() == null
                ? "auto"
                : properties.getWebhookAuthMode().trim().toLowerCase(Locale.ROOT);
        boolean apiKeyHeader = isApiKeyAuthorization(authorizationHeader);
        boolean hmacHeaders = timestampHeader != null
                && !timestampHeader.isBlank()
                && signatureHeader != null
                && !signatureHeader.isBlank();
        if ("hmac".equals(mode)) {
            return parseAndVerifyWebhook(
                    properties.getWebhookSecret(),
                    properties.getMaxTimestampSkewSeconds(),
                    timestampHeader,
                    signatureHeader,
                    rawBody);
        }
        if ("apikey".equals(mode) || ("auto".equals(mode) && apiKeyHeader)) {
            assertApiKey(properties.getApiKey(), authorizationHeader);
            return parseBody(rawBody);
        }
        if ("auto".equals(mode) && hmacHeaders) {
            return parseAndVerifyWebhook(
                    properties.getWebhookSecret(),
                    properties.getMaxTimestampSkewSeconds(),
                    timestampHeader,
                    signatureHeader,
                    rawBody);
        }
        throw new AuthException("Webhook SePay không hợp lệ.");
    }

    static void assertApiKey(String expected, String authorizationHeader) {
        if (expected == null || expected.isBlank()) {
            throw new BusinessRuleException("Thiếu cấu hình API key của SePay.");
        }
        if (!isApiKeyAuthorization(authorizationHeader)) {
            throw new AuthException("Webhook SePay không hợp lệ.");
        }
        String provided = authorizationHeader.trim().substring("Apikey ".length()).trim();
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
            throw new AuthException("API key webhook SePay không hợp lệ.");
        }
    }

    private static boolean isApiKeyAuthorization(String authorizationHeader) {
        return authorizationHeader != null
                && authorizationHeader.trim().regionMatches(true, 0, "Apikey ", 0, "Apikey ".length());
    }

    private static SepayWebhookPayload parseBody(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            throw new BusinessRuleException("Webhook payload trống.");
        }
        try {
            return OBJECT_MAPPER.readValue(rawBody, SepayWebhookPayload.class);
        } catch (JsonProcessingException e) {
            throw new BusinessRuleException("Webhook payload không phải JSON hợp lệ.");
        }
    }

    private String generateOrderCode() {
        return "HST" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
    }

    private String buildQrUrl(String orderCode, int amount) {
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

    static String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Cannot compute SePay HMAC", e);
        }
    }

    private String toJson(SepayWebhookPayload payload) {
        return payload == null
                ? "{}"
                : "{"
                        + "\"id\":" + payload.id() + ","
                        + "\"code\":\"" + safe(payload.code()) + "\","
                        + "\"gateway\":\"" + safe(payload.gateway()) + "\","
                        + "\"referenceCode\":\"" + safe(payload.referenceCode()) + "\","
                        + "\"content\":\"" + safe(payload.content()) + "\""
                        + "}";
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }
}

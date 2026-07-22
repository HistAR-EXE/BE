package com.histar.be.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.auth.service.EmailVerifiedGuard;
import com.histar.be.billing.entity.B2cPaymentTransaction;
import com.histar.be.billing.repository.B2cPaymentTransactionRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.SepayProperties;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SepayB2cPaymentServiceTest {

    @Mock
    private B2cPaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private BillingService billingService;

    @Mock
    private BillingSettingsService billingSettingsService;

    @Mock
    private EmailVerifiedGuard emailVerifiedGuard;

    @InjectMocks
    private SepayB2cPaymentService sepayB2cPaymentService;

    @BeforeEach
    void setUp() {
        SepayProperties props = new SepayProperties();
        props.setEnabled(true);
        props.setBankCode("Vietcombank");
        props.setAccountNumber("0010000000355");
        props.setAccountName("CONG TY HISTAR");
        props.setWebhookSecret("whsec-test-secret");
        props.setMaxTimestampSkewSeconds(300);
        props.setQrTemplate("compact");
        props.setQrShowInfo(true);
        ReflectionTestUtils.setField(sepayB2cPaymentService, "sepayProperties", props);
    }

    @Test
    void createPayment_rejectsOrgUser() {
        UUID userId = UUID.randomUUID();
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).orgId(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> sepayB2cPaymentService.createPayment(userId, "/settings"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("tổ chức");
    }

    @Test
    void handleWebhook_marksPaymentPaidAndUpgradesUser() {
        UUID userId = UUID.randomUUID();
        B2cPaymentTransaction tx = B2cPaymentTransaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .provider("SEPAY")
                .orderCode("HSTABC123")
                .transferContent("HSTABC123")
                .amountVnd(79_000)
                .status("PENDING")
                .expiresAt(Instant.now().plusSeconds(600))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(paymentTransactionRepository.findByProviderTransactionId(12345L)).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByOrderCode("HSTABC123")).thenReturn(Optional.of(tx));

        String rawBody = """
                {"id":12345,"gateway":"Vietcombank","transactionDate":"2024-07-02 11:08:33","accountNumber":"0010000000355","subAccount":"","code":"HSTABC123","content":"HSTABC123 chuyen tien","transferType":"in","description":"Thanh toan","transferAmount":79000,"accumulated":100000,"referenceCode":"FT2401"}
                """.trim();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = "sha256=" + SepayB2cPaymentService.hmacSha256Hex("whsec-test-secret", timestamp + "." + rawBody);

        sepayB2cPaymentService.handleWebhook(
                timestamp,
                signature,
                rawBody);

        assertThat(tx.getStatus()).isEqualTo("PAID");
        assertThat(tx.getProviderTransactionId()).isEqualTo(12345L);
        verify(paymentTransactionRepository).save(tx);
        verify(billingService).subscribeB2c(userId, "SEPAY");
    }

    @Test
    void handleWebhook_ignoresDuplicateTransactionId() {
        when(paymentTransactionRepository.findByProviderTransactionId(999L))
                .thenReturn(Optional.of(B2cPaymentTransaction.builder().build()));

        String rawBody = """
                {"id":999,"gateway":"VCB","transactionDate":"2024-07-02 11:08:33","accountNumber":"","subAccount":"","code":"HST1","content":"HST1","transferType":"in","description":"","transferAmount":79000,"accumulated":0,"referenceCode":""}
                """.trim();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = "sha256=" + SepayB2cPaymentService.hmacSha256Hex("whsec-test-secret", timestamp + "." + rawBody);

        sepayB2cPaymentService.handleWebhook(
                timestamp,
                signature,
                rawBody);

        verify(paymentTransactionRepository, never()).findByOrderCode(any());
        verify(billingService, never()).subscribeB2c(any(), any());
    }

    @Test
    void parseAndVerifyWebhook_rejectsInvalidSignature() {
        String rawBody = "{\"id\":1}";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        assertThatThrownBy(() -> SepayB2cPaymentService.parseAndVerifyWebhook(
                        "whsec-test-secret",
                        300,
                        timestamp,
                        "sha256=invalid",
                        rawBody))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Chữ ký");
    }
}

package com.histar.be.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.auth.service.EmailVerifiedGuard;
import com.histar.be.billing.dto.OrgBillingStatus;
import com.histar.be.billing.dto.OrgCreatePaymentRequest;
import com.histar.be.billing.entity.OrgPaymentTransaction;
import com.histar.be.billing.repository.OrgPaymentTransactionRepository;
import com.histar.be.config.SepayProperties;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.time.LocalDate;
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
class SepayOrgPaymentServiceTest {

    @Mock
    private OrgPaymentTransactionRepository orgPaymentTransactionRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private BillingService billingService;

    @Mock
    private BillingSettingsService billingSettingsService;

    @Mock
    private EmailVerifiedGuard emailVerifiedGuard;

    @InjectMocks
    private SepayOrgPaymentService sepayOrgPaymentService;

    @BeforeEach
    void setUp() {
        SepayProperties props = new SepayProperties();
        props.setEnabled(true);
        props.setBankCode("MBBank");
        props.setAccountNumber("0815330544");
        props.setAccountName("DANG THUAN PHAT");
        props.setWebhookSecret("whsec-test-secret");
        props.setMaxTimestampSkewSeconds(300);
        props.setQrTemplate("compact");
        props.setQrShowInfo(true);
        ReflectionTestUtils.setField(sepayOrgPaymentService, "sepayProperties", props);
        ReflectionTestUtils.setField(sepayOrgPaymentService, "billingSettingsService", billingSettingsService);
    }

    @Test
    void createPayment_createsPendingOrgIntent() {
        UUID userId = UUID.randomUUID();
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).email("teacher@school.vn").build()));
        when(orgPaymentTransactionRepository.findFirstByRequesterUserIdAndStatusOrderByCreatedAtDesc(userId, "PENDING"))
                .thenReturn(Optional.empty());
        when(orgPaymentTransactionRepository.save(any(OrgPaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(billingSettingsService.calculateOrgVolumePricing(15_000_000L, 1))
                .thenReturn(new BillingSettingsService.OrgVolumePricing(15_000_000L, 0, 0L, 15_000_000L, 1));

        var response = sepayOrgPaymentService.createPayment(
                userId,
                new OrgCreatePaymentRequest("THPT Test", "STANDARD", "teacher@school.vn", null, "/teacher", 1));

        assertThat(response.orderCode()).startsWith("ORG");
        assertThat(response.amountVnd()).isEqualTo(15_000_000);
        assertThat(response.planType()).isEqualTo("STANDARD");
    }

    @Test
    void handleWebhook_marksOrgPaymentPaidAndActivatesOrg() {
        UUID userId = UUID.randomUUID();
        OrgPaymentTransaction tx = OrgPaymentTransaction.builder()
                .id(UUID.randomUUID())
                .requesterUserId(userId)
                .orgName("THPT Test")
                .contactEmail("teacher@school.vn")
                .planType("STANDARD")
                .provider("SEPAY")
                .orderCode("ORGABC123")
                .transferContent("ORGABC123")
                .amountVnd(15_000_000L)
                .status("PENDING")
                .expiresAt(Instant.now().plusSeconds(600))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(orgPaymentTransactionRepository.findByProviderTransactionId(223344L)).thenReturn(Optional.empty());
        when(orgPaymentTransactionRepository.findByOrderCode("ORGABC123")).thenReturn(Optional.of(tx));
        when(orgPaymentTransactionRepository.save(any(OrgPaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(billingService.subscribeOrg(any(), any(), any()))
                .thenReturn(new OrgBillingStatus(
                        UUID.randomUUID(),
                        "THPT Test",
                        "STANDARD",
                        LocalDate.now().plusYears(1),
                        true,
                        0,
                        30000,
                        LocalDate.now().plusMonths(1),
                        1,
                        400,
                        false,
                        0,
                        40,
                        false,
                        365));

        String rawBody = """
                {"id":223344,"gateway":"MBBank","transactionDate":"2026-07-07 16:00:00","accountNumber":"0815330544","subAccount":"","code":"ORGABC123","content":"ORGABC123 thanh toan","transferType":"in","description":"Thanh toan","transferAmount":15000000,"accumulated":100000,"referenceCode":"FT2402"}
                """.trim();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = "sha256=" + SepayB2cPaymentService.hmacSha256Hex("whsec-test-secret", timestamp + "." + rawBody);

        sepayOrgPaymentService.handleWebhook(timestamp, signature, rawBody);

        assertThat(tx.getStatus()).isEqualTo("PAID");
        assertThat(tx.getProviderTransactionId()).isEqualTo(223344L);
        verify(orgPaymentTransactionRepository, atLeastOnce()).save(tx);
        verify(billingService).subscribeOrg(any(), any(), any());
    }
}

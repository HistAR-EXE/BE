package com.histar.be.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.auth.service.EmailVerifiedGuard;
import com.histar.be.billing.dto.SepayWebhookPayload;
import com.histar.be.billing.entity.B2cPaymentTransaction;
import com.histar.be.billing.entity.B2cSubscription;
import com.histar.be.billing.entity.B2cVisitEntitlement;
import com.histar.be.billing.repository.B2cPaymentTransactionRepository;
import com.histar.be.billing.repository.B2cSubscriptionRepository;
import com.histar.be.billing.repository.B2cVisitEntitlementRepository;
import com.histar.be.mail.HistarEmailService;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.SepayProperties;
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

    @Mock
    private B2cSubscriptionRepository b2cSubscriptionRepository;

    @Mock
    private B2cVisitEntitlementRepository visitEntitlementRepository;

    @Mock
    private HistarEmailService histarEmailService;

    @Mock private com.histar.be.location.repository.LocationRepository locationRepository;
    @Mock private com.histar.be.story.repository.StoryChapterRepository storyChapterRepository;
    @Mock private com.histar.be.profile.service.TierAccessService tierAccessService;

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
        props.setApiKey("sk-test-key");
        props.setWebhookAuthMode("auto");
        props.setMaxTimestampSkewSeconds(300);
        props.setQrTemplate("compact");
        props.setQrShowInfo(true);
        ReflectionTestUtils.setField(sepayB2cPaymentService, "sepayProperties", props);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"cu-chi","hoang-thanh-thang-long","dai-noi-hue"})
    void journeyPassAllowsImplementedPilotSite(String site) {
        UUID id=UUID.randomUUID();
        var tx=pendingTx(id,"PASS"+site);
        tx.setPlanType("JOURNEY_PASS"); tx.setSiteCode(site); tx.setAmountVnd(29000);
        when(profileRepository.findById(id)).thenReturn(Optional.of(Profile.builder().id(id).tier("FREE").build()));
        when(locationRepository.findBySiteCodeIgnoreCase(site)).thenReturn(Optional.of(com.histar.be.location.entity.Location.builder().id(UUID.randomUUID()).siteCode(site).build()));
        when(storyChapterRepository.findBySiteCodeOrderBySortOrderAscChapterNumberAsc(site)).thenReturn(java.util.List.of(com.histar.be.story.entity.StoryChapter.builder().requiresPremium(true).build()));
        when(billingSettingsService.getB2cJourneyPassPriceVnd()).thenReturn(29000);
        when(paymentTransactionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(id,"PENDING")).thenReturn(Optional.of(tx));
        assertThat(sepayB2cPaymentService.createPayment(id,"/explore","JOURNEY_PASS",site).orderCode()).isEqualTo(tx.getOrderCode());
    }

    @Test
    void journeyPassDoesNotReusePendingOrderForAnotherSite() {
        UUID id=UUID.randomUUID(); String site="dai-noi-hue";
        var old=pendingTx(id,"OLD-CUCHI"); old.setPlanType("JOURNEY_PASS"); old.setSiteCode("cu-chi");
        when(profileRepository.findById(id)).thenReturn(Optional.of(Profile.builder().id(id).tier("FREE").build()));
        when(locationRepository.findBySiteCodeIgnoreCase(site)).thenReturn(Optional.of(com.histar.be.location.entity.Location.builder().id(UUID.randomUUID()).siteCode(site).build()));
        when(storyChapterRepository.findBySiteCodeOrderBySortOrderAscChapterNumberAsc(site)).thenReturn(java.util.List.of(com.histar.be.story.entity.StoryChapter.builder().requiresPremium(true).build()));
        when(billingSettingsService.getB2cJourneyPassPriceVnd()).thenReturn(29000);
        when(paymentTransactionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(id,"PENDING")).thenReturn(Optional.of(old));
        when(paymentTransactionRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        var intent=sepayB2cPaymentService.createPayment(id,"/explore","JOURNEY_PASS",site);
        assertThat(intent.orderCode()).isNotEqualTo("OLD-CUCHI");
        verify(paymentTransactionRepository).save(org.mockito.ArgumentMatchers.argThat(tx -> site.equals(tx.getSiteCode()) && tx.getAmountVnd()==29000));
    }

    @Test
    void journeyPassRejectsUnsupportedSiteBeforeCreatingPayment() {
        UUID id=UUID.randomUUID();
        when(profileRepository.findById(id)).thenReturn(Optional.of(Profile.builder().id(id).tier("FREE").build()));
        assertThatThrownBy(() -> sepayB2cPaymentService.createPayment(id,"/explore","JOURNEY_PASS","invented-site"))
            .isInstanceOf(BusinessRuleException.class).hasMessageContaining("hỗ trợ");
        verify(paymentTransactionRepository, never()).save(any());
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
                .amountVnd(49_000)
                .status("PENDING")
                .expiresAt(Instant.now().plusSeconds(600))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(paymentTransactionRepository.findByProviderTransactionId(12345L)).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByOrderCodeForUpdate("HSTABC123")).thenReturn(Optional.of(tx));

        String rawBody = """
                {"id":12345,"gateway":"Vietcombank","transactionDate":"2024-07-02 11:08:33","accountNumber":"0010000000355","subAccount":"","code":"HSTABC123","content":"HSTABC123 chuyen tien","transferType":"in","description":"Thanh toan","transferAmount":49000,"accumulated":100000,"referenceCode":"FT2401"}
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
        verify(billingService).subscribeB2cReturningSubscription(userId, "SEPAY");
    }

    @Test
    void handleWebhook_ignoresDuplicateTransactionId() {
        when(paymentTransactionRepository.findByProviderTransactionId(999L))
                .thenReturn(Optional.of(B2cPaymentTransaction.builder().build()));

        String rawBody = """
                {"id":999,"gateway":"VCB","transactionDate":"2024-07-02 11:08:33","accountNumber":"","subAccount":"","code":"HST1","content":"HST1","transferType":"in","description":"","transferAmount":49000,"accumulated":0,"referenceCode":""}
                """.trim();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = "sha256=" + SepayB2cPaymentService.hmacSha256Hex("whsec-test-secret", timestamp + "." + rawBody);

        sepayB2cPaymentService.handleWebhook(
                timestamp,
                signature,
                rawBody);

        verify(paymentTransactionRepository, never()).findByOrderCode(any());
        verify(billingService, never()).subscribeB2cReturningSubscription(any(), any());
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

    @Test
    void handleWebhook_acceptsSepayApiKeyAuthorization() {
        UUID userId = UUID.randomUUID();
        B2cPaymentTransaction tx = B2cPaymentTransaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .provider("SEPAY")
                .orderCode("HSTAPIKEY1")
                .transferContent("HSTAPIKEY1")
                .amountVnd(49_000)
                .status("PENDING")
                .expiresAt(Instant.now().plusSeconds(600))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(paymentTransactionRepository.findByProviderTransactionId(77L)).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByOrderCodeForUpdate("HSTAPIKEY1")).thenReturn(Optional.of(tx));

        String rawBody = """
                {"id":77,"gateway":"MBBank","transactionDate":"2024-07-02 11:08:33","accountNumber":"0815330544","subAccount":"","code":"HSTAPIKEY1","content":"HSTAPIKEY1","transferType":"in","description":"Thanh toan","transferAmount":49000,"accumulated":100000,"referenceCode":"FT77"}
                """.trim();

        sepayB2cPaymentService.handleWebhook(null, null, "Apikey sk-test-key", rawBody);

        assertThat(tx.getStatus()).isEqualTo("PAID");
        verify(billingService).subscribeB2cReturningSubscription(userId, "SEPAY");
    }

    @Test
    void handleWebhook_linksSubscriptionAndSendsReceipt() {
        UUID userId = UUID.randomUUID();
        UUID subId = UUID.randomUUID();
        B2cPaymentTransaction tx = pendingTx(userId, "HSTLINK001");
        when(paymentTransactionRepository.findByProviderTransactionId(501L)).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByOrderCodeForUpdate("HSTLINK001")).thenReturn(Optional.of(tx));
        when(billingService.subscribeB2cReturningSubscription(userId, "SEPAY"))
                .thenReturn(B2cSubscription.builder()
                        .id(subId)
                        .userId(userId)
                        .endDate(LocalDate.now().plusMonths(1))
                        .build());
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).email("a@b.vn").build()));

        sepayB2cPaymentService.handleWebhook(null, null, "Apikey sk-test-key", payloadBody(501L, "HSTLINK001", 49000));

        assertThat(tx.getStatus()).isEqualTo("PAID");
        assertThat(tx.getSubscriptionId()).isEqualTo(subId);
        verify(histarEmailService).sendText(eq("a@b.vn"), any(), any());
    }

    @Test
    void handleWebhook_underpaidSetsUnderpaidAndDoesNotUpgrade() {
        UUID userId = UUID.randomUUID();
        B2cPaymentTransaction tx = pendingTx(userId, "HSTUNDER01");
        when(paymentTransactionRepository.findByProviderTransactionId(502L)).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByOrderCodeForUpdate("HSTUNDER01")).thenReturn(Optional.of(tx));

        sepayB2cPaymentService.handleWebhook(null, null, "Apikey sk-test-key", payloadBody(502L, "HSTUNDER01", 10000));

        assertThat(tx.getStatus()).isEqualTo("UNDERPAID");
        assertThat(tx.getProviderPayload()).contains("HSTUNDER01");
        verify(paymentTransactionRepository).save(tx);
        verify(billingService, never()).subscribeB2cReturningSubscription(any(), any());
    }

    @Test
    void getPaymentStatus_underpaidNotMappedToExpiredEvenWhenPastExpiry() {
        UUID userId = UUID.randomUUID();
        B2cPaymentTransaction tx = pendingTx(userId, "HSTUNDER02");
        tx.setStatus("UNDERPAID");
        tx.setExpiresAt(Instant.now().minusSeconds(3600));
        tx.setProviderPayload("{\"receivedAmount\":10000}");
        when(paymentTransactionRepository.findByOrderCode("HSTUNDER02")).thenReturn(Optional.of(tx));

        var response = sepayB2cPaymentService.getPaymentStatus(userId, "HSTUNDER02");

        assertThat(response.status()).isEqualTo("UNDERPAID");
        assertThat(response.receivedAmountVnd()).isEqualTo(10_000);
        assertThat(response.remainingAmountVnd()).isEqualTo(39_000);
        assertThat(response.transferContent()).isEqualTo("HSTUNDER02");
    }

    @Test
    void handleWebhook_topUpOnUnderpaidOrderCompletesPayment() {
        UUID userId = UUID.randomUUID();
        B2cPaymentTransaction tx = pendingTx(userId, "HSTTOPUP01");
        tx.setStatus("UNDERPAID");
        tx.setProviderPayload("{\"id\":1,\"receivedAmount\":10000}");
        when(paymentTransactionRepository.findByProviderTransactionId(601L)).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByOrderCodeForUpdate("HSTTOPUP01")).thenReturn(Optional.of(tx));

        sepayB2cPaymentService.handleWebhook(null, null, "Apikey sk-test-key", payloadBody(601L, "HSTTOPUP01", 39000));

        assertThat(tx.getStatus()).isEqualTo("PAID");
        verify(billingService).subscribeB2cReturningSubscription(userId, "SEPAY");
    }

    @Test
    void handleWebhook_doesNotReopenRefundedPayment() {
        UUID userId = UUID.randomUUID();
        B2cPaymentTransaction tx = pendingTx(userId, "HSTREFUND1");
        tx.setStatus("REFUNDED");
        when(paymentTransactionRepository.findByProviderTransactionId(602L)).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByOrderCodeForUpdate("HSTREFUND1")).thenReturn(Optional.of(tx));

        sepayB2cPaymentService.handleVerifiedWebhook(new SepayWebhookPayload(
                602L, "MBBank", null, null, null, "HSTREFUND1", "HSTREFUND1",
                "in", null, 49_000L, null, "FT602"));

        assertThat(tx.getStatus()).isEqualTo("REFUNDED");
        verify(paymentTransactionRepository, never()).save(tx);
        verify(billingService, never()).subscribeB2cReturningSubscription(any(), any());
    }

    @Test
    void handleVerifiedWebhookLocksPaymentBeforeFinalization() {
        UUID userId = UUID.randomUUID();
        B2cPaymentTransaction tx = pendingTx(userId, "HSTLOCK001");
        when(paymentTransactionRepository.findByProviderTransactionId(603L)).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findByOrderCodeForUpdate("HSTLOCK001")).thenReturn(Optional.of(tx));

        sepayB2cPaymentService.handleVerifiedWebhook(new SepayWebhookPayload(
                603L, "MBBank", null, null, null, "HSTLOCK001", "HSTLOCK001",
                "in", null, 49_000L, null, "FT603"));

        assertThat(tx.getStatus()).isEqualTo("PAID");
        verify(paymentTransactionRepository).findByOrderCodeForUpdate("HSTLOCK001");
        verify(billingService).subscribeB2cReturningSubscription(userId, "SEPAY");
    }

    @Test
    void finalizePaidB2cDoesNotGrantSubscriptionTwice() {
        UUID userId = UUID.randomUUID();
        B2cPaymentTransaction tx = paidTx(userId, "HSTIDEMPOTENT1");
        tx.setSubscriptionId(UUID.randomUUID());

        sepayB2cPaymentService.finalizePaidB2c(tx, false);

        verify(billingService, never()).subscribeB2cReturningSubscription(any(), any());
    }

    @Test
    void finalizePaidJourneyPassDoesNotGrantEntitlementTwice() {
        B2cPaymentTransaction tx = paidTx(UUID.randomUUID(), "HSTIDEMPOTENT2");
        tx.setPlanType(SepayB2cPaymentService.PLAN_JOURNEY_PASS);
        when(visitEntitlementRepository.findByOrderCode(tx.getOrderCode()))
                .thenReturn(java.util.List.of(B2cVisitEntitlement.builder().id(UUID.randomUUID()).build()));

        sepayB2cPaymentService.finalizePaidB2c(tx, false);

        verify(visitEntitlementRepository, never()).save(any());
    }

    @Test
    void createPayment_reusesUnderpaidOrderWithinGrace() {
        UUID userId = UUID.randomUUID();
        B2cPaymentTransaction tx = pendingTx(userId, "HSTREUSE01");
        tx.setStatus("UNDERPAID");
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).tier("FREE").build()));
        when(billingSettingsService.getB2cPremiumPriceVnd()).thenReturn(49_000);
        when(paymentTransactionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, "PENDING"))
                .thenReturn(Optional.empty());
        when(paymentTransactionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, "UNDERPAID"))
                .thenReturn(Optional.of(tx));

        var intent = sepayB2cPaymentService.createPayment(userId, "/home");

        assertThat(intent.orderCode()).isEqualTo("HSTREUSE01");
        assertThat(intent.status()).isEqualTo("UNDERPAID");
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void getPaymentStatus_upgradedWhenPaidAndProfilePremium() {
        UUID userId = UUID.randomUUID();
        B2cPaymentTransaction tx = paidTx(userId, "HSTPAID001");
        when(paymentTransactionRepository.findByOrderCode("HSTPAID001")).thenReturn(Optional.of(tx));
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).tier("PREMIUM").build()));

        when(tierAccessService.hasPersonalPremiumAccess(any())).thenReturn(true);
        assertThat(sepayB2cPaymentService.getPaymentStatus(userId, "HSTPAID001").upgraded()).isTrue();
    }

    @Test
    void getPaymentStatus_upgradedWhenPaidAndActiveSubscription() {
        UUID userId = UUID.randomUUID();
        B2cPaymentTransaction tx = paidTx(userId, "HSTPAID002");
        when(paymentTransactionRepository.findByOrderCode("HSTPAID002")).thenReturn(Optional.of(tx));
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).tier("FREE").build()));
        when(tierAccessService.hasPersonalPremiumAccess(any())).thenReturn(true);

        assertThat(sepayB2cPaymentService.getPaymentStatus(userId, "HSTPAID002").upgraded()).isTrue();
    }

    @Test
    void getPaymentStatus_notUpgradedWhenPaidButNoEntitlement() {
        UUID userId = UUID.randomUUID();
        B2cPaymentTransaction tx = paidTx(userId, "HSTPAID003");
        when(paymentTransactionRepository.findByOrderCode("HSTPAID003")).thenReturn(Optional.of(tx));
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).tier("FREE").build()));

        var response = sepayB2cPaymentService.getPaymentStatus(userId, "HSTPAID003");

        assertThat(response.status()).isEqualTo("PAID");
        assertThat(response.upgraded()).isFalse();
    }

    @Test
    void getPaymentStatus_notUpgradedWhenPending() {
        UUID userId = UUID.randomUUID();
        B2cPaymentTransaction tx = pendingTx(userId, "HSTPEND001");
        when(paymentTransactionRepository.findByOrderCode("HSTPEND001")).thenReturn(Optional.of(tx));

        assertThat(sepayB2cPaymentService.getPaymentStatus(userId, "HSTPEND001").upgraded()).isFalse();
    }

    private static B2cPaymentTransaction pendingTx(UUID userId, String orderCode) {
        return B2cPaymentTransaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .provider("SEPAY")
                .orderCode(orderCode)
                .transferContent(orderCode)
                .amountVnd(49_000)
                .status("PENDING")
                .expiresAt(Instant.now().plusSeconds(600))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private static B2cPaymentTransaction paidTx(UUID userId, String orderCode) {
        B2cPaymentTransaction tx = pendingTx(userId, orderCode);
        tx.setStatus("PAID");
        tx.setPaidAt(Instant.now());
        return tx;
    }

    private static String payloadBody(long id, String code, int amount) {
        return "{\"id\":" + id + ",\"gateway\":\"MBBank\",\"transactionDate\":\"2024-07-02 11:08:33\","
                + "\"accountNumber\":\"0815330544\",\"subAccount\":\"\",\"code\":\"" + code + "\","
                + "\"content\":\"" + code + "\",\"transferType\":\"in\",\"description\":\"\","
                + "\"transferAmount\":" + amount + ",\"accumulated\":0,\"referenceCode\":\"FT\"}";
    }

    @Test
    void handleWebhook_rejectsWrongApiKey() {
        String rawBody = "{\"id\":1,\"transferType\":\"in\",\"code\":\"HST1\",\"transferAmount\":49000}";

        assertThatThrownBy(() -> sepayB2cPaymentService.handleWebhook(null, null, "Apikey wrong-key", rawBody))
                .isInstanceOf(AuthException.class);
        verify(billingService, never()).subscribeB2cReturningSubscription(any(), any());
    }
}

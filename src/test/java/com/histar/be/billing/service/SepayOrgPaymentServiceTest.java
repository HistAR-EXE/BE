package com.histar.be.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.auth.service.EmailVerifiedGuard;
import com.histar.be.billing.dto.OrgBillingStatus;
import com.histar.be.billing.dto.OrgCreatePaymentRequest;
import com.histar.be.billing.dto.SepayWebhookPayload;
import com.histar.be.billing.entity.OrgPaymentTransaction;
import com.histar.be.billing.entity.OrgBillingSubscription;
import com.histar.be.billing.repository.OrgBillingSubscriptionRepository;
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
    private OrgBillingSubscriptionRepository orgBillingSubscriptionRepository;

    @Mock
    private EmailVerifiedGuard emailVerifiedGuard;

    @Mock
    private com.histar.be.organization.repository.OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private OrgPaidActivationService paidActivation;

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
        when(orgPaymentTransactionRepository.findFirstByRequesterUserIdAndStatusOrderByCreatedAtDesc(userId, "UNDERPAID"))
                .thenReturn(Optional.empty());
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
        when(orgPaymentTransactionRepository.findByOrderCodeForUpdate("ORGABC123")).thenReturn(Optional.of(tx));
        when(orgPaymentTransactionRepository.save(any(OrgPaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        UUID orgId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        when(billingService.subscribeOrg(any(), any(), any()))
                .thenReturn(new OrgBillingStatus(
                        orgId,
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
        OrgBillingSubscription subscription = OrgBillingSubscription.builder()
                .id(subscriptionId).organizationId(orgId).isActive(true).build();
        when(orgBillingSubscriptionRepository.findAllByOrganizationIdOrderByCreatedAtDesc(orgId))
                .thenReturn(java.util.List.of(subscription));
        tx.setLicenseCount(25);
        tx.setGrossAmountVnd(375_000_000L);
        tx.setDiscountAmountVnd(37_500_000L);
        tx.setAmountVnd(337_500_000L);

        String rawBody = """
                {"id":223344,"gateway":"MBBank","transactionDate":"2026-07-07 16:00:00","accountNumber":"0815330544","subAccount":"","code":"ORGABC123","content":"ORGABC123 thanh toan","transferType":"in","description":"Thanh toan","transferAmount":337500000,"accumulated":100000,"referenceCode":"FT2402"}
                """.trim();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = "sha256=" + SepayB2cPaymentService.hmacSha256Hex("whsec-test-secret", timestamp + "." + rawBody);

        sepayOrgPaymentService.handleWebhook(timestamp, signature, rawBody);
        verify(paidActivation).activate(tx.getId());
        // Activation is invoked in its own transaction after receipt commit.
        sepayOrgPaymentService.activatePaidOrgOnce(tx);

        assertThat(tx.getStatus()).isEqualTo("PAID");
        assertThat(tx.getProviderTransactionId()).isEqualTo(223344L);
        assertThat(tx.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(subscription.getSourcePaymentTransactionId()).isEqualTo(tx.getId());
        assertThat(subscription.getBillingCadence()).isEqualTo("ANNUAL");
        assertThat(subscription.getContractValueVnd()).isEqualTo(337_500_000L);
        assertThat(subscription.getCurrency()).isEqualTo("VND");
        assertThat(subscription.getQuantity()).isEqualTo(25);
        verify(orgPaymentTransactionRepository, atLeastOnce()).save(tx);
        verify(billingService).subscribeOrg(any(), any(), any());
    }

    @Test
    void handleWebhook_doesNotReopenVoidedPayment() {
        OrgPaymentTransaction tx = OrgPaymentTransaction.builder()
                .id(UUID.randomUUID()).requesterUserId(UUID.randomUUID()).orderCode("ORGVOID001")
                .transferContent("ORGVOID001").amountVnd(1_000_000L).status("VOIDED")
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(orgPaymentTransactionRepository.findByProviderTransactionId(223345L)).thenReturn(Optional.empty());
        when(orgPaymentTransactionRepository.findByOrderCodeForUpdate("ORGVOID001")).thenReturn(Optional.of(tx));

        sepayOrgPaymentService.handleVerifiedWebhook(new SepayWebhookPayload(
                223345L, "MBBank", null, null, null, "ORGVOID001", "ORGVOID001",
                "in", null, 1_000_000L, null, "FT223345"));

        assertThat(tx.getStatus()).isEqualTo("VOIDED");
        verify(orgPaymentTransactionRepository, never()).save(tx);
        verify(billingService, never()).subscribeOrg(any(), any(), any());
    }

    @Test
    void handleVerifiedWebhookLocksPaymentBeforeFinalization() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        OrgPaymentTransaction tx = OrgPaymentTransaction.builder()
                .id(UUID.randomUUID()).requesterUserId(userId).orgName("THPT Test")
                .contactEmail("teacher@school.vn").planType("STANDARD").provider("SEPAY")
                .orderCode("ORGLOCK001").transferContent("ORGLOCK001").amountVnd(1_000_000L)
                .licenseCount(1).status("PENDING").createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(orgPaymentTransactionRepository.findByProviderTransactionId(223346L)).thenReturn(Optional.empty());
        when(orgPaymentTransactionRepository.findByOrderCodeForUpdate("ORGLOCK001")).thenReturn(Optional.of(tx));
        when(billingService.subscribeOrg(any(), any(), any())).thenReturn(new OrgBillingStatus(
                orgId, "THPT Test", "STANDARD", LocalDate.now().plusYears(1), true,
                0, 30_000, LocalDate.now().plusMonths(1), 1, 400, false, 0, 40, false, 365));
        OrgBillingSubscription subscription = OrgBillingSubscription.builder()
                .id(UUID.randomUUID()).organizationId(orgId).isActive(true).build();
        when(orgBillingSubscriptionRepository.findAllByOrganizationIdOrderByCreatedAtDesc(orgId))
                .thenReturn(java.util.List.of(subscription));

        sepayOrgPaymentService.handleVerifiedWebhook(new SepayWebhookPayload(
                223346L, "MBBank", null, null, null, "ORGLOCK001", "ORGLOCK001",
                "in", null, 1_000_000L, null, "FT223346"));

        verify(paidActivation).activate(tx.getId());
        sepayOrgPaymentService.activatePaidOrgOnce(tx);
        assertThat(tx.getStatus()).isEqualTo("PAID");
        verify(orgPaymentTransactionRepository).findByOrderCodeForUpdate("ORGLOCK001");
        verify(billingService).subscribeOrg(any(), any(), any());
    }

    @Test
    void finalizePaidOrgDoesNotGrantSubscriptionTwice() {
        OrgPaymentTransaction tx = OrgPaymentTransaction.builder()
                .id(UUID.randomUUID()).requesterUserId(UUID.randomUUID()).subscriptionId(UUID.randomUUID())
                .orgName("THPT Test").contactEmail("teacher@school.vn").planType("STANDARD")
                .amountVnd(1_000_000L).status("PAID").build();

        sepayOrgPaymentService.finalizePaidOrg(tx);

        verify(billingService, never()).subscribeOrg(any(), any(), any());
    }

    @Test
    void createPaymentRejectsReusingIntentWhenRequestedPricingDiffersFromSnapshot() {
        UUID userId = UUID.randomUUID();
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).email("teacher@school.vn").build()));
        OrgPaymentTransaction pending = OrgPaymentTransaction.builder()
                .requesterUserId(userId).orgName("THPT Test").contactEmail("teacher@school.vn")
                .planType("STANDARD").provider("SEPAY").orderCode("OLD").transferContent("OLD")
                .amountVnd(14_000_000L).licenseCount(1).grossAmountVnd(14_000_000L).discountAmountVnd(0L)
                .status("PENDING").expiresAt(Instant.now().plusSeconds(600)).build();
        when(orgPaymentTransactionRepository.findFirstByRequesterUserIdAndStatusOrderByCreatedAtDesc(userId, "UNDERPAID"))
                .thenReturn(Optional.empty());
        when(orgPaymentTransactionRepository.findFirstByRequesterUserIdAndStatusOrderByCreatedAtDesc(userId, "PENDING"))
                .thenReturn(Optional.of(pending));
        when(billingSettingsService.calculateOrgVolumePricing(15_000_000L, 1))
                .thenReturn(new BillingSettingsService.OrgVolumePricing(15_000_000L, 0, 0L, 15_000_000L, 1));

        assertThatThrownBy(() -> sepayOrgPaymentService.createPayment(userId,
                new OrgCreatePaymentRequest("THPT Test", "STANDARD", "teacher@school.vn", null, "/teacher", 1)))
                .isInstanceOf(com.histar.be.common.exception.BusinessRuleException.class)
                .hasMessageContaining("không khớp");
    }
    @Test
    void unrelatedActorCannotReserveSeatsOnAnotherOrganization() {
        UUID user = UUID.randomUUID(), orgId = UUID.randomUUID();
        when(profileRepository.findById(user)).thenReturn(Optional.of(Profile.builder().id(user).email("outsider@test.local").build()));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(com.histar.be.organization.entity.Organization.builder().id(orgId).build()));
        assertThatThrownBy(() -> sepayOrgPaymentService.createPayment(user,
                new OrgCreatePaymentRequest("Target", "MICRO", "outsider@test.local", orgId, "/teacher", 1)))
                .isInstanceOf(com.histar.be.common.exception.BusinessRuleException.class).hasMessageContaining("Chỉ chủ tổ chức");
        verify(orgPaymentTransactionRepository, never()).save(any());
        verify(organizationRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void acceptedStudentCannotCreateOrganizationPaymentIntent() {
        UUID user = UUID.randomUUID(), orgId = UUID.randomUUID();
        when(profileRepository.findById(user)).thenReturn(Optional.of(Profile.builder().id(user).orgId(orgId).email("student@test.local").build()));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(com.histar.be.organization.entity.Organization.builder().id(orgId).build()));
        when(organizationMemberRepository.findByOrganizationIdAndUserId(orgId,user)).thenReturn(Optional.of(
                com.histar.be.organization.entity.OrganizationMember.builder().organizationId(orgId).userId(user).orgRole("student").acceptedAt(Instant.now()).build()));
        assertThatThrownBy(() -> sepayOrgPaymentService.createPayment(user,
                new OrgCreatePaymentRequest("Target", "MICRO", "student@test.local", orgId, "/teacher", 1)))
                .isInstanceOf(com.histar.be.common.exception.BusinessRuleException.class).hasMessageContaining("Chỉ chủ tổ chức");
        verify(orgPaymentTransactionRepository, never()).save(any());
    }

    @Test
    void explicitMissingOrganizationCannotBecomeAChargeForNewOrganization() {
        UUID user=UUID.randomUUID(), orgId=UUID.randomUUID();
        when(profileRepository.findById(user)).thenReturn(Optional.of(Profile.builder().id(user).email("owner@test.local").build()));
        assertThatThrownBy(() -> sepayOrgPaymentService.createPayment(user,
                new OrgCreatePaymentRequest("Missing", "MICRO", "owner@test.local", orgId, "/teacher", 1)))
                .isInstanceOf(com.histar.be.common.exception.ResourceNotFoundException.class);
        verify(orgPaymentTransactionRepository,never()).save(any());
    }

    @Test
    void statusPollUsesLockedFreshPaymentBeforeRetryingActivation() {
        UUID user=UUID.randomUUID(), subscription=UUID.randomUUID();
        var completed=OrgPaymentTransaction.builder().id(UUID.randomUUID()).requesterUserId(user)
                .subscriptionId(subscription).status("PAID").orderCode("PAIDLOCK").amountVnd(8_000_000L).build();
        when(orgPaymentTransactionRepository.findByOrderCodeForUpdate("PAIDLOCK")).thenReturn(Optional.of(completed));
        var first=sepayOrgPaymentService.getPaymentStatus(user,"PAIDLOCK");
        var second=sepayOrgPaymentService.getPaymentStatus(user,"PAIDLOCK");
        assertThat(first.activated()).isTrue(); assertThat(second.activated()).isTrue();
        verify(orgPaymentTransactionRepository,org.mockito.Mockito.times(2)).findByOrderCodeForUpdate("PAIDLOCK");
        verify(orgPaymentTransactionRepository,never()).findByOrderCode(any());
        verify(billingService,never()).subscribeOrg(any(),any(),any());
    }

    @Test
    void pendingOrderForAnotherOrganizationCannotBeReused() {
        UUID user=UUID.randomUUID(), requestedOrg=UUID.randomUUID(), oldOrg=UUID.randomUUID();
        when(profileRepository.findById(user)).thenReturn(Optional.of(Profile.builder().id(user).email("admin@test.local").role("ADMIN").build()));
        var requested=com.histar.be.organization.entity.Organization.builder().id(requestedOrg).name("Requested").build();
        when(organizationRepository.findById(requestedOrg)).thenReturn(Optional.of(requested));
        when(organizationRepository.findByIdForUpdate(requestedOrg)).thenReturn(Optional.of(requested));
        var pending=OrgPaymentTransaction.builder().id(UUID.randomUUID()).requesterUserId(user).organizationId(oldOrg)
                .orgName("Other").contactEmail("other@test.local").planType("MICRO").orderCode("OLDORG")
                .amountVnd(8000000L).grossAmountVnd(8000000L).discountAmountVnd(0L).licenseCount(1)
                .status("PENDING").expiresAt(Instant.now().plusSeconds(300)).build();
        when(orgPaymentTransactionRepository.findFirstByRequesterUserIdAndStatusOrderByCreatedAtDesc(user,"UNDERPAID")).thenReturn(Optional.empty());
        when(orgPaymentTransactionRepository.findFirstByRequesterUserIdAndStatusOrderByCreatedAtDesc(user,"PENDING")).thenReturn(Optional.of(pending));
        when(billingSettingsService.calculateOrgVolumePricing(8000000L,1)).thenReturn(new BillingSettingsService.OrgVolumePricing(8000000L,0,0L,8000000L,1));
        when(orgPaymentTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var result=sepayOrgPaymentService.createPayment(user,new OrgCreatePaymentRequest("Requested","MICRO","admin@test.local",requestedOrg,null,1));
        assertThat(result.orderCode()).isNotEqualTo("OLDORG");assertThat(result.organizationId()).isEqualTo(requestedOrg);
    }

}

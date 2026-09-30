package com.histar.be.billing.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.auth.service.EmailVerifiedGuard;
import com.histar.be.billing.entity.B2cSubscription;
import com.histar.be.billing.service.CcuSessionService;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.DemoProperties;
import com.histar.be.organization.entity.Organization;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private com.histar.be.billing.repository.B2cSubscriptionRepository b2cSubscriptionRepository;

    @Mock
    private com.histar.be.billing.repository.OrgBillingSubscriptionRepository orgBillingSubscriptionRepository;

    @Mock
    private com.histar.be.organization.repository.OrganizationRepository organizationRepository;

    @Mock
    private com.histar.be.organization.repository.OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private com.histar.be.profile.service.ProfileMeService profileMeService;

    @Mock
    private UsageQuotaService usageQuotaService;

    @Mock
    private BillingSettingsService billingSettingsService;

    @Mock
    private EmailVerifiedGuard emailVerifiedGuard;

    @Mock
    private CcuSessionService ccuSessionService;

    @Mock
    private DemoProperties demoProperties;

    @InjectMocks
    private BillingService billingService;

    @Test
    void subscribeB2c_rejectsDemoWhenDemoDisabled() {
        UUID userId = UUID.randomUUID();
        when(demoProperties.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> billingService.subscribeB2c(userId, "DEMO"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("demo");
        verify(profileRepository, never()).findById(any());
    }

    @Test
    void subscribeB2cReturningSubscription_allowsSepayWhenDemoDisabled() {
        UUID userId = UUID.randomUUID();
        UUID subId = UUID.randomUUID();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(Profile.builder().id(userId).build()));
        when(billingSettingsService.getB2cPremiumPriceVnd()).thenReturn(49_000);
        when(b2cSubscriptionRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(Optional.empty());
        when(b2cSubscriptionRepository.save(any(B2cSubscription.class))).thenAnswer(inv -> {
            B2cSubscription s = inv.getArgument(0);
            s.setId(subId);
            return s;
        });

        B2cSubscription created = billingService.subscribeB2cReturningSubscription(userId, "SEPAY");

        assertThat(created.getId()).isEqualTo(subId);
        assertThat(created.getPaymentMethod()).isEqualTo("SEPAY");
    }

    @Test
    void reconcileExpiredB2cSubscriptions_deactivatesAndDowngradesFreeTier() {
        UUID userId = UUID.randomUUID();
        B2cSubscription expired = B2cSubscription.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .isActive(true)
                .endDate(LocalDate.now().minusDays(1))
                .build();
        Profile profile = Profile.builder().id(userId).tier("PREMIUM").build();
        when(b2cSubscriptionRepository.findAllByIsActiveTrueAndEndDateBefore(any(LocalDate.class)))
                .thenReturn(List.of(expired));
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        int affected = billingService.reconcileExpiredB2cSubscriptions();

        assertThat(affected).isEqualTo(1);
        assertThat(expired.getIsActive()).isFalse();
        assertThat(profile.getTier()).isEqualTo("FREE");
    }

    @Test
    void subscribeB2c_rejectsOrgMember() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(demoProperties.isEnabled()).thenReturn(true);
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).orgId(orgId).build()));

        assertThatThrownBy(() -> billingService.subscribeB2c(userId, "DEMO"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("tổ chức");
    }

    @Test
    void getB2cHistory_returnsNewestFirst() {
        UUID userId = UUID.randomUUID();
        when(b2cSubscriptionRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(
                        B2cSubscription.builder()
                                .id(UUID.randomUUID())
                                .startDate(LocalDate.now().minusMonths(1))
                                .endDate(LocalDate.now())
                                .isActive(false)
                                .priceVnd(79_000)
                                .paymentMethod("DEMO")
                                .createdAt(Instant.now())
                                .build()));

        var history = billingService.getB2cHistory(userId);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).priceVnd()).isEqualTo(79_000);
        assertThat(history.get(0).paymentMethod()).isEqualTo("DEMO");
    }

    @Test
    void reconcileExpiredTrials_marksPastTrialActiveAsTrialExpired() {
        UUID expiredId = UUID.randomUUID();
        UUID activeTrialId = UUID.randomUUID();
        UUID paidExpiredId = UUID.randomUUID();
        LocalDate yesterday = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1);
        LocalDate nextWeek = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(7);

        when(organizationRepository.findAll())
                .thenReturn(List.of(
                        Organization.builder()
                                .id(expiredId)
                                .status(BillingService.ORG_STATUS_TRIAL_ACTIVE)
                                .planEndDate(yesterday)
                                .build(),
                        Organization.builder()
                                .id(activeTrialId)
                                .status(BillingService.ORG_STATUS_TRIAL_ACTIVE)
                                .planEndDate(nextWeek)
                                .build(),
                        Organization.builder()
                                .id(paidExpiredId)
                                .status(BillingService.ORG_STATUS_ACTIVE)
                                .planEndDate(yesterday)
                                .build()));

        int affected = billingService.reconcileExpiredTrials();

        assertThat(affected).isEqualTo(1);
        verify(organizationRepository)
                .save(argThat(org -> expiredId.equals(org.getId())
                        && BillingService.ORG_STATUS_TRIAL_EXPIRED.equalsIgnoreCase(org.getStatus())));
    }
}

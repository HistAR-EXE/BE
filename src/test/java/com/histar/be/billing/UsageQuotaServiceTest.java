package com.histar.be.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.billing.entity.UsageQuota;
import com.histar.be.billing.repository.UsageQuotaRepository;
import com.histar.be.common.exception.QuotaExceededException;
import com.histar.be.config.HistarOrgProperties;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.profile.service.TierAccessService;
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
class UsageQuotaServiceTest {

    @Mock
    private UsageQuotaRepository usageQuotaRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private BillingSettingsService billingSettingsService;

    @Mock
    private TierAccessService tierAccessService;

    @InjectMocks
    private UsageQuotaService usageQuotaService;

    private final HistarOrgProperties histarOrgProperties = new HistarOrgProperties();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(usageQuotaService, "histarOrgProperties", histarOrgProperties);
        ReflectionTestUtils.setField(usageQuotaService, "legacyDailyLimit", 10);
    }

    private void stubDailyLimit() {
        when(billingSettingsService.getChatFreeDailyLimit()).thenReturn(10);
    }

    @Test
    void assertCanSendChat_throwsWhenFreeUserExceedsDailyLimit() {
        stubDailyLimit();
        UUID userId = UUID.randomUUID();
        Profile profile = Profile.builder().id(userId).tier(UserTier.FREE.name()).role("USER").build();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(usageQuotaRepository.findByUserIdAndYearAndMonthAndDay(any(), anyInt(), anyInt(), anyInt()))
                .thenReturn(Optional.of(UsageQuota.builder().usedAiQueries(10).build()));

        assertThatThrownBy(() -> usageQuotaService.assertCanSendChat(userId))
                .isInstanceOf(QuotaExceededException.class);
    }

    @Test
    void assertCanSendChat_skipsForOrgMemberUsingOrgQuota() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Profile profile = Profile.builder()
                .id(userId)
                .tier(UserTier.FREE.name())
                .role("ORG_MEMBER")
                .orgId(orgId)
                .build();
        Organization org = Organization.builder()
                .id(orgId)
                .maxAiQueriesPerMonth(5000)
                .maxVerifiedAccounts(100)
                .build();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(usageQuotaRepository.findByOrganizationIdAndYearAndMonth(any(), anyInt(), anyInt()))
                .thenReturn(Optional.of(UsageQuota.builder().usedAiQueries(100).build()));

        usageQuotaService.assertCanSendChat(userId);
        verify(usageQuotaRepository, never()).findByUserIdAndYearAndMonthAndDay(any(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void assertCanSendChat_allowsPremiumUser() {
        UUID userId = UUID.randomUUID();
        Profile profile = Profile.builder().id(userId).tier(UserTier.PREMIUM.name()).role("USER").build();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        usageQuotaService.assertCanSendChat(userId);
        verify(usageQuotaRepository, never()).findByUserIdAndYearAndMonthAndDay(any(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void assertCanSendChat_teacherUsesOrgQuotaInsteadOfUnlimitedByRole() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Profile profile = Profile.builder()
                .id(userId)
                .tier(UserTier.FREE.name())
                .role("TEACHER")
                .orgId(orgId)
                .build();
        Organization org = Organization.builder()
                .id(orgId)
                .planType("STANDARD")
                .maxAiQueriesPerMonth(30_000)
                .build();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(usageQuotaRepository.findByOrganizationIdAndYearAndMonth(any(), anyInt(), anyInt()))
                .thenReturn(Optional.of(UsageQuota.builder().usedAiQueries(30_000).build()));

        assertThatThrownBy(() -> usageQuotaService.assertCanSendChat(userId))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("Tổ chức đã dùng hết")
                .satisfies(ex -> {
                    QuotaExceededException quota = (QuotaExceededException) ex;
                    assertThat(quota.getQuotaType()).isEqualTo("ORG_MONTHLY");
                    assertThat(quota.getUpgradePackage()).isEqualTo("PREMIUM");
                });
    }

    @Test
    void shouldIncludeChatSources_trueForJourneyPassAtSite() {
        UUID userId = UUID.randomUUID();
        Profile profile = Profile.builder().id(userId).tier(UserTier.FREE.name()).role("USER").build();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(tierAccessService.hasActiveJourneyPass(userId, "cu-chi")).thenReturn(true);

        assertThat(usageQuotaService.shouldIncludeChatSources(userId, "cu-chi")).isTrue();
    }

    @Test
    void shouldIncludeChatSources_falseForFreeWithoutPass() {
        UUID userId = UUID.randomUUID();
        Profile profile = Profile.builder().id(userId).tier(UserTier.FREE.name()).role("USER").build();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(tierAccessService.hasActiveJourneyPass(userId, "cu-chi")).thenReturn(false);

        assertThat(usageQuotaService.shouldIncludeChatSources(userId, "cu-chi")).isFalse();
    }
}

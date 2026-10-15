package com.histar.be.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.billing.dto.UpdateBillingSettingsRequest;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.billing.entity.BillingSetting;
import com.histar.be.billing.repository.BillingSettingRepository;
import com.histar.be.config.SepayProperties;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class BillingSettingsServiceTest {

    @Mock
    private BillingSettingRepository billingSettingRepository;

    @Mock
    private SepayProperties sepayProperties;

    @Mock
    private AdminAuditService audit;

    @Mock
    private CurrentUserAccessor currentUser;

    @InjectMocks
    private BillingSettingsService billingSettingsService;

    @Test
    void getB2cPremiumPriceVnd_usesFallbackFromEnvWhenDbMissing() {
        when(billingSettingRepository.findById(BillingSettingsService.B2C_PREMIUM_PRICE_KEY))
                .thenReturn(Optional.empty());
        when(sepayProperties.getB2cPremiumPriceVnd()).thenReturn(79_000);

        assertThat(billingSettingsService.getB2cPremiumPriceVnd()).isEqualTo(79_000);
    }

    @Test
    void updateSettings_persistsNewPrice() {
        when(currentUser.getUserId()).thenReturn(Optional.of(UUID.randomUUID()));
        when(billingSettingRepository.findById(BillingSettingsService.B2C_PREMIUM_PRICE_KEY))
                .thenReturn(Optional.of(BillingSetting.builder()
                        .settingKey(BillingSettingsService.B2C_PREMIUM_PRICE_KEY)
                        .settingValue("79000")
                        .build()));
        when(billingSettingRepository.findById(BillingSettingsService.CHAT_FREE_DAILY_LIMIT_KEY))
                .thenReturn(Optional.of(BillingSetting.builder()
                        .settingKey(BillingSettingsService.CHAT_FREE_DAILY_LIMIT_KEY)
                        .settingValue("10")
                        .build()));
        when(billingSettingRepository.findById(BillingSettingsService.ORG_VOLUME_DISCOUNT_PERCENT_KEY))
                .thenReturn(Optional.empty());
        when(billingSettingRepository.findById(BillingSettingsService.ORG_VOLUME_DISCOUNT_MIN_LICENSES_KEY))
                .thenReturn(Optional.empty());
        when(sepayProperties.getBankCode()).thenReturn("MBBank");
        when(sepayProperties.getAccountNumber()).thenReturn("0815330544");
        when(sepayProperties.getAccountName()).thenReturn("DANG THUAN PHAT");
        when(sepayProperties.getQrTemplate()).thenReturn("compact");
        when(sepayProperties.isQrShowInfo()).thenReturn(true);

        var response = billingSettingsService.updateSettings(new UpdateBillingSettingsRequest(150000, null, null, null));

        assertThat(response.b2cPremiumPriceVnd()).isEqualTo(150000);
        assertThat(response.chatFreeDailyLimit()).isEqualTo(10);
        verify(billingSettingRepository).save(org.mockito.ArgumentMatchers.argThat(
                setting -> BillingSettingsService.B2C_PREMIUM_PRICE_KEY.equals(setting.getSettingKey())));
        verify(audit).append(org.mockito.ArgumentMatchers.argThat(command ->
                "BILLING_SETTINGS".equals(command.entityType())
                        && "SETTINGS_UPDATED".equals(command.action())));
    }

    @Test
    void updateSettings_persistsChatFreeDailyLimit() {
        when(currentUser.getUserId()).thenReturn(Optional.of(UUID.randomUUID()));
        when(billingSettingRepository.findById(BillingSettingsService.B2C_PREMIUM_PRICE_KEY))
                .thenReturn(Optional.of(BillingSetting.builder()
                        .settingKey(BillingSettingsService.B2C_PREMIUM_PRICE_KEY)
                        .settingValue("79000")
                        .build()));
        when(billingSettingRepository.findById(eq(BillingSettingsService.CHAT_FREE_DAILY_LIMIT_KEY)))
                .thenReturn(Optional.empty(), Optional.of(BillingSetting.builder()
                        .settingKey(BillingSettingsService.CHAT_FREE_DAILY_LIMIT_KEY)
                        .settingValue("8")
                        .build()));
        when(billingSettingRepository.findById(BillingSettingsService.ORG_VOLUME_DISCOUNT_PERCENT_KEY))
                .thenReturn(Optional.empty());
        when(billingSettingRepository.findById(BillingSettingsService.ORG_VOLUME_DISCOUNT_MIN_LICENSES_KEY))
                .thenReturn(Optional.empty());
        when(sepayProperties.getBankCode()).thenReturn("MBBank");
        when(sepayProperties.getAccountNumber()).thenReturn("0815330544");
        when(sepayProperties.getAccountName()).thenReturn("DANG THUAN PHAT");
        when(sepayProperties.getQrTemplate()).thenReturn("compact");
        when(sepayProperties.isQrShowInfo()).thenReturn(true);

        var response = billingSettingsService.updateSettings(new UpdateBillingSettingsRequest(null, 8, null, null));

        assertThat(response.chatFreeDailyLimit()).isEqualTo(8);
    }

    @Test
    void updateSettings_acceptsStitchSliderRangeUpTo50() {
        when(currentUser.getUserId()).thenReturn(Optional.of(UUID.randomUUID()));
        when(billingSettingRepository.findById(BillingSettingsService.B2C_PREMIUM_PRICE_KEY))
                .thenReturn(Optional.of(BillingSetting.builder()
                        .settingKey(BillingSettingsService.B2C_PREMIUM_PRICE_KEY)
                        .settingValue("49000")
                        .build()));
        when(billingSettingRepository.findById(eq(BillingSettingsService.CHAT_FREE_DAILY_LIMIT_KEY)))
                .thenReturn(Optional.empty(), Optional.of(BillingSetting.builder()
                        .settingKey(BillingSettingsService.CHAT_FREE_DAILY_LIMIT_KEY)
                        .settingValue("17")
                        .build()));
        when(billingSettingRepository.findById(BillingSettingsService.ORG_VOLUME_DISCOUNT_PERCENT_KEY))
                .thenReturn(Optional.empty());
        when(billingSettingRepository.findById(BillingSettingsService.ORG_VOLUME_DISCOUNT_MIN_LICENSES_KEY))
                .thenReturn(Optional.empty());
        when(sepayProperties.getBankCode()).thenReturn("MBBank");
        when(sepayProperties.getAccountNumber()).thenReturn("0815330544");
        when(sepayProperties.getAccountName()).thenReturn("DANG THUAN PHAT");
        when(sepayProperties.getQrTemplate()).thenReturn("compact");
        when(sepayProperties.isQrShowInfo()).thenReturn(true);

        var response = billingSettingsService.updateSettings(new UpdateBillingSettingsRequest(null, 17, null, null));

        assertThat(response.chatFreeDailyLimit()).isEqualTo(17);
        verify(billingSettingRepository).save(org.mockito.ArgumentMatchers.argThat(
                setting -> BillingSettingsService.CHAT_FREE_DAILY_LIMIT_KEY.equals(setting.getSettingKey())
                        && "17".equals(setting.getSettingValue())));
    }

    @Test
    void updateSettings_rejectsChatLimitOutsideStitchRange() {
        when(currentUser.getUserId()).thenReturn(Optional.of(UUID.randomUUID()));

        assertThatThrownBy(() ->
                        billingSettingsService.updateSettings(new UpdateBillingSettingsRequest(null, 51, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("3")
                .hasMessageContaining("50");
    }
}

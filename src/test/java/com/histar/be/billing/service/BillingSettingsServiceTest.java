package com.histar.be.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.billing.dto.UpdateBillingSettingsRequest;
import com.histar.be.billing.entity.BillingSetting;
import com.histar.be.billing.repository.BillingSettingRepository;
import com.histar.be.config.SepayProperties;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingSettingsServiceTest {

    @Mock
    private BillingSettingRepository billingSettingRepository;

    @Mock
    private SepayProperties sepayProperties;

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
    }

    @Test
    void updateSettings_persistsChatFreeDailyLimit() {
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
}

package com.histar.be.billing.service;

import com.histar.be.billing.dto.AdminBillingSettingsResponse;
import com.histar.be.billing.dto.UpdateBillingSettingsRequest;
import com.histar.be.billing.entity.BillingSetting;
import com.histar.be.billing.repository.BillingSettingRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.SepayProperties;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingSettingsService {

    public static final String B2C_PREMIUM_PRICE_KEY = "b2c_premium_price_vnd";
    public static final String CHAT_FREE_DAILY_LIMIT_KEY = "chat_free_daily_limit";
    public static final String ORG_VOLUME_DISCOUNT_PERCENT_KEY = "org_volume_discount_percent";
    public static final String ORG_VOLUME_DISCOUNT_MIN_LICENSES_KEY = "org_volume_discount_min_licenses";
    private static final int DEFAULT_CHAT_FREE_DAILY_LIMIT = 10;
    private static final int DEFAULT_VOLUME_DISCOUNT_PERCENT = 35;
    private static final int DEFAULT_VOLUME_DISCOUNT_MIN_LICENSES = 3;
    private static final int MIN_CHAT_FREE_DAILY_LIMIT = 5;
    private static final int MAX_CHAT_FREE_DAILY_LIMIT = 10;

    private final BillingSettingRepository billingSettingRepository;
    private final SepayProperties sepayProperties;

    @Transactional(readOnly = true)
    public int getB2cPremiumPriceVnd() {
        return billingSettingRepository.findById(B2C_PREMIUM_PRICE_KEY)
                .map(BillingSetting::getSettingValue)
                .map(Integer::parseInt)
                .orElseGet(sepayProperties::getB2cPremiumPriceVnd);
    }

    @Transactional(readOnly = true)
    public int getOrgVolumeDiscountPercent() {
        return billingSettingRepository.findById(ORG_VOLUME_DISCOUNT_PERCENT_KEY)
                .map(BillingSetting::getSettingValue)
                .map(this::parsePositiveInt)
                .orElse(DEFAULT_VOLUME_DISCOUNT_PERCENT);
    }

    @Transactional(readOnly = true)
    public int getOrgVolumeDiscountMinLicenses() {
        return billingSettingRepository.findById(ORG_VOLUME_DISCOUNT_MIN_LICENSES_KEY)
                .map(BillingSetting::getSettingValue)
                .map(this::parsePositiveInt)
                .orElse(DEFAULT_VOLUME_DISCOUNT_MIN_LICENSES);
    }

    public record OrgVolumePricing(long subtotalVnd, int discountPercent, long discountAmountVnd, long totalVnd, int licenseCount) {}

    @Transactional(readOnly = true)
    public OrgVolumePricing calculateOrgVolumePricing(long unitPriceVnd, int licenseCount) {
        int count = Math.max(1, licenseCount);
        long subtotal = unitPriceVnd * count;
        int minLicenses = getOrgVolumeDiscountMinLicenses();
        int discountPercent = count >= minLicenses ? getOrgVolumeDiscountPercent() : 0;
        long discountAmount = discountPercent > 0 ? Math.round(subtotal * discountPercent / 100.0) : 0L;
        long total = subtotal - discountAmount;
        return new OrgVolumePricing(subtotal, discountPercent, discountAmount, total, count);
    }

    private int parsePositiveInt(String raw) {
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : 1;
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    @Transactional(readOnly = true)
    public int getChatFreeDailyLimit() {
        return billingSettingRepository.findById(CHAT_FREE_DAILY_LIMIT_KEY)
                .map(BillingSetting::getSettingValue)
                .map(this::parseChatFreeDailyLimit)
                .orElse(DEFAULT_CHAT_FREE_DAILY_LIMIT);
    }

    @Transactional(readOnly = true)
    public AdminBillingSettingsResponse getAdminSettings() {
        BillingSetting priceSetting = billingSettingRepository.findById(B2C_PREMIUM_PRICE_KEY).orElse(null);
        return new AdminBillingSettingsResponse(
                getB2cPremiumPriceVnd(),
                getChatFreeDailyLimit(),
                getOrgVolumeDiscountPercent(),
                getOrgVolumeDiscountMinLicenses(),
                sepayProperties.getBankCode(),
                sepayProperties.getAccountNumber(),
                sepayProperties.getAccountName(),
                sepayProperties.getQrTemplate(),
                sepayProperties.isQrShowInfo(),
                priceSetting == null ? null : priceSetting.getUpdatedAt());
    }

    @Transactional
    public AdminBillingSettingsResponse updateSettings(UpdateBillingSettingsRequest request) {
        if (request.b2cPremiumPriceVnd() == null
                && request.chatFreeDailyLimit() == null
                && request.orgVolumeDiscountPercent() == null
                && request.orgVolumeDiscountMinLicenses() == null) {
            throw new BusinessRuleException("Cần ít nhất một giá trị cài đặt để cập nhật.");
        }
        if (request.b2cPremiumPriceVnd() != null && request.b2cPremiumPriceVnd() < 1000) {
            throw new BusinessRuleException("Giá B2C Premium phải từ 1.000 VND trở lên.");
        }
        if (request.chatFreeDailyLimit() != null) {
            validateChatFreeDailyLimit(request.chatFreeDailyLimit());
        }
        if (request.b2cPremiumPriceVnd() != null) {
            saveSetting(B2C_PREMIUM_PRICE_KEY, String.valueOf(request.b2cPremiumPriceVnd()));
        }
        if (request.chatFreeDailyLimit() != null) {
            saveSetting(CHAT_FREE_DAILY_LIMIT_KEY, String.valueOf(request.chatFreeDailyLimit()));
        }
        if (request.orgVolumeDiscountPercent() != null) {
            saveSetting(ORG_VOLUME_DISCOUNT_PERCENT_KEY, String.valueOf(request.orgVolumeDiscountPercent()));
        }
        if (request.orgVolumeDiscountMinLicenses() != null) {
            saveSetting(ORG_VOLUME_DISCOUNT_MIN_LICENSES_KEY, String.valueOf(request.orgVolumeDiscountMinLicenses()));
        }
        return getAdminSettings();
    }

    private void saveSetting(String key, String value) {
        BillingSetting setting = billingSettingRepository.findById(key)
                .orElse(BillingSetting.builder().settingKey(key).build());
        setting.setSettingValue(value);
        setting.setUpdatedAt(Instant.now());
        billingSettingRepository.save(setting);
    }

    private int parseChatFreeDailyLimit(String raw) {
        try {
            return validateChatFreeDailyLimit(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ex) {
            return DEFAULT_CHAT_FREE_DAILY_LIMIT;
        }
    }

    private int validateChatFreeDailyLimit(int limit) {
        if (limit < MIN_CHAT_FREE_DAILY_LIMIT || limit > MAX_CHAT_FREE_DAILY_LIMIT) {
            throw new BusinessRuleException(
                    "Giới hạn chat FREE phải từ " + MIN_CHAT_FREE_DAILY_LIMIT + " đến " + MAX_CHAT_FREE_DAILY_LIMIT + ".");
        }
        return limit;
    }
}

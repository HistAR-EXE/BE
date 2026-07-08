package com.histar.be.billing.service;

import com.histar.be.billing.entity.UsageQuota;
import com.histar.be.billing.repository.UsageQuotaRepository;
import com.histar.be.billing.service.BillingSettingsService;
import com.histar.be.common.exception.QuotaExceededException;
import com.histar.be.config.HistarOrgProperties;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.entity.OrgSubscription;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsageQuotaService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String UPGRADE_URL = "/pricing";

    private final UsageQuotaRepository usageQuotaRepository;
    private final ProfileRepository profileRepository;
    private final OrganizationRepository organizationRepository;
    private final HistarOrgProperties histarOrgProperties;
    private final BillingSettingsService billingSettingsService;

    @Value("${gemini.daily-message-limit:10}")
    private int legacyDailyLimit;

    @Transactional(readOnly = true)
    public void assertCanSendChat(UUID userId) {
        Profile profile = profileRepository.findById(userId).orElse(null);
        if (profile == null || hasUnlimitedChat(profile)) {
            return;
        }

        if (profile.getOrgId() != null) {
            assertOrgMonthlyQuota(profile.getOrgId());
            return;
        }

        int limit = resolveFreeDailyLimit();
        int used = getUserDailyUsage(userId);
        if (used >= limit) {
            throw new QuotaExceededException(
                    "Đã đạt giới hạn " + limit + " tin nhắn/ngày. Nâng cấp Premium để chat không giới hạn.",
                    UPGRADE_URL,
                    "B2C_DAILY",
                    null);
        }
    }

    @Transactional(readOnly = true)
    public boolean shouldIncludeChatSources(UUID userId) {
        Profile profile = profileRepository.findById(userId).orElse(null);
        return profile != null && hasUnlimitedChat(profile);
    }

    @Transactional
    public void recordChatSuccess(UUID userId) {
        Profile profile = profileRepository.findById(userId).orElse(null);
        if (profile == null || hasUnlimitedChat(profile)) {
            return;
        }

        LocalDate vnDay = LocalDate.now(VN_ZONE);
        if (profile.getOrgId() != null) {
            incrementOrgMonthly(profile.getOrgId(), vnDay);
            return;
        }

        incrementUserDaily(userId, vnDay);
    }

    @Transactional(readOnly = true)
    public int getUserDailyUsage(UUID userId) {
        LocalDate vnDay = LocalDate.now(VN_ZONE);
        return usageQuotaRepository
                .findByUserIdAndYearAndMonthAndDay(userId, vnDay.getYear(), vnDay.getMonthValue(), vnDay.getDayOfMonth())
                .map(UsageQuota::getUsedAiQueries)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public int getOrgMonthlyUsage(UUID orgId) {
        LocalDate vnDay = LocalDate.now(VN_ZONE);
        return usageQuotaRepository
                .findByOrganizationIdAndYearAndMonth(orgId, vnDay.getYear(), vnDay.getMonthValue())
                .map(UsageQuota::getUsedAiQueries)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public LocalDate getUserDailyResetDate() {
        return LocalDate.now(VN_ZONE).plusDays(1);
    }

    @Transactional(readOnly = true)
    public LocalDate getOrgMonthlyResetDate() {
        LocalDate vnDay = LocalDate.now(VN_ZONE);
        return vnDay.withDayOfMonth(1).plusMonths(1);
    }

    @Transactional(readOnly = true)
    public int getUserDailyLimit(Profile profile) {
        if (hasUnlimitedChat(profile)) {
            return -1;
        }
        if (profile.getOrgId() != null) {
            Organization org = organizationRepository.findById(profile.getOrgId()).orElse(null);
            if (org != null && org.getMaxAiQueriesPerMonth() == null) {
                return -1;
            }
            return org != null && org.getMaxAiQueriesPerMonth() != null ? org.getMaxAiQueriesPerMonth() : 5_000;
        }
        return resolveFreeDailyLimit();
    }

    private int resolveFreeDailyLimit() {
        int fromDb = billingSettingsService.getChatFreeDailyLimit();
        if (fromDb > 0) {
            return fromDb;
        }
        return histarOrgProperties.getTier().getFreeChatDailyLimit() > 0
                ? histarOrgProperties.getTier().getFreeChatDailyLimit()
                : legacyDailyLimit;
    }

    public int mockCcu(UUID orgId) {
        int hash = Math.abs(orgId.hashCode() % 7);
        return 3 + hash;
    }

    private void assertOrgMonthlyQuota(UUID orgId) {
        Organization org = organizationRepository.findById(orgId).orElse(null);
        if (org == null || isOrgPlanExpired(org)) {
            throw new QuotaExceededException(
                    "Gói tổ chức đã hết hạn hoặc không hợp lệ. Liên hệ quản trị viên trường.",
                    UPGRADE_URL,
                    "ORG_MONTHLY",
                    null);
        }
        Integer limit = org.getMaxAiQueriesPerMonth();
        if (limit == null) {
            return;
        }
        int used = getOrgMonthlyUsage(orgId);
        if (used >= limit) {
            throw new QuotaExceededException(
                    "Tổ chức đã dùng hết " + limit + " lượt AI trong tháng. Liên hệ giáo viên hoặc nâng gói.",
                    UPGRADE_URL,
                    "ORG_MONTHLY",
                    suggestOrgUpgradePackage(org));
        }
    }

    private static String suggestOrgUpgradePackage(Organization org) {
        String plan = org.getPlanType() != null ? org.getPlanType() : org.getPlan();
        if (plan == null) {
            return "STANDARD";
        }
        return switch (plan.toUpperCase()) {
            case "MICRO" -> "STANDARD";
            case "STANDARD" -> "PREMIUM";
            default -> "PREMIUM";
        };
    }

    @Transactional(readOnly = true)
    public boolean hasPremiumEntitlement(UUID userId) {
        Profile profile = profileRepository.findById(userId).orElse(null);
        if (profile == null) {
            return false;
        }
        UserRole role = UserRole.fromStored(profile.getRole());
        if (role == UserRole.ADMIN) {
            return true;
        }
        if (profile.getOrgId() != null) {
            Organization org = organizationRepository.findById(profile.getOrgId()).orElse(null);
            return org != null && !isOrgPlanExpired(org);
        }
        return UserTier.PREMIUM == UserTier.fromStored(profile.getTier());
    }

    private boolean hasUnlimitedChat(Profile profile) {
        UserRole role = UserRole.fromStored(profile.getRole());
        if (role == UserRole.ADMIN) {
            return true;
        }
        if (role == UserRole.TEACHER && profile.getOrgId() == null) {
            return true;
        }
        if (UserTier.PREMIUM == UserTier.fromStored(profile.getTier()) && profile.getOrgId() == null) {
            return true;
        }
        if (profile.getOrgId() != null) {
            Organization org = organizationRepository.findById(profile.getOrgId()).orElse(null);
            if (org != null && !isOrgPlanExpired(org)) {
                OrgSubscription plan = OrgSubscription.fromPlanType(
                        org.getPlanType() != null ? org.getPlanType() : org.getPlan());
                if (plan == OrgSubscription.PREMIUM || org.getMaxAiQueriesPerMonth() == null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isOrgPlanExpired(Organization org) {
        if (org.getPlanEndDate() == null) {
            return false;
        }
        return org.getPlanEndDate().isBefore(LocalDate.now(VN_ZONE));
    }

    private void incrementUserDaily(UUID userId, LocalDate vnDay) {
        UsageQuota quota = usageQuotaRepository
                .findByUserIdAndYearAndMonthAndDay(
                        userId, vnDay.getYear(), vnDay.getMonthValue(), vnDay.getDayOfMonth())
                .orElseGet(() -> UsageQuota.builder()
                        .userId(userId)
                        .year(vnDay.getYear())
                        .month(vnDay.getMonthValue())
                        .day(vnDay.getDayOfMonth())
                        .usedAiQueries(0)
                        .build());
        quota.setUsedAiQueries(quota.getUsedAiQueries() + 1);
        usageQuotaRepository.save(quota);
    }

    private void incrementOrgMonthly(UUID orgId, LocalDate vnDay) {
        UsageQuota quota = usageQuotaRepository
                .findByOrganizationIdAndYearAndMonth(orgId, vnDay.getYear(), vnDay.getMonthValue())
                .orElseGet(() -> UsageQuota.builder()
                        .organizationId(orgId)
                        .year(vnDay.getYear())
                        .month(vnDay.getMonthValue())
                        .usedAiQueries(0)
                        .build());
        quota.setUsedAiQueries(quota.getUsedAiQueries() + 1);
        usageQuotaRepository.save(quota);
    }
}

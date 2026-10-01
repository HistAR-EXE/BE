package com.histar.be.billing.service;

import com.histar.be.auth.service.EmailVerifiedGuard;
import com.histar.be.billing.policy.OrgPlanLimits;
import com.histar.be.billing.dto.B2cBillingStatus;
import com.histar.be.billing.dto.B2cSubscriptionHistoryItem;
import com.histar.be.billing.dto.BillingStatusResponse;
import com.histar.be.billing.dto.BillingPublicPricingResponse;
import com.histar.be.billing.dto.OrgBillingStatus;
import com.histar.be.billing.dto.OrgPlanInfo;
import com.histar.be.billing.dto.OrgSubscriptionHistoryItem;
import com.histar.be.billing.dto.OrgSubscribeRequest;
import com.histar.be.billing.dto.OrgTrialRequest;
import com.histar.be.billing.entity.B2cSubscription;
import com.histar.be.billing.entity.OrgBillingSubscription;
import com.histar.be.billing.repository.B2cSubscriptionRepository;
import com.histar.be.billing.repository.OrgBillingSubscriptionRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.config.DemoProperties;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.entity.OrganizationMember;
import com.histar.be.organization.entity.OrgSubscription;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.profile.service.ProfileMeService;
import com.histar.be.profile.dto.ProfileMeResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final String PAYMENT_METHOD_DEMO = "DEMO";
    public static final String ORG_STATUS_ACTIVE = "ACTIVE";
    public static final String ORG_STATUS_EXPIRED = "EXPIRED";
    public static final String ORG_STATUS_TRIAL_ACTIVE = "TRIAL_ACTIVE";
    public static final String ORG_STATUS_TRIAL_EXPIRED = "TRIAL_EXPIRED";

    private final ProfileRepository profileRepository;
    private final B2cSubscriptionRepository b2cSubscriptionRepository;
    private final OrgBillingSubscriptionRepository orgBillingSubscriptionRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final ProfileMeService profileMeService;
    private final UsageQuotaService usageQuotaService;
    private final BillingSettingsService billingSettingsService;
    private final CcuSessionService ccuSessionService;
    private final EmailVerifiedGuard emailVerifiedGuard;
    private final DemoProperties demoProperties;

    public static boolean isOrgActive(Organization org) {
        if (org == null) {
            return false;
        }
        if (ORG_STATUS_EXPIRED.equalsIgnoreCase(org.getStatus())
                || ORG_STATUS_TRIAL_EXPIRED.equalsIgnoreCase(org.getStatus())
                || "SUSPENDED".equalsIgnoreCase(org.getStatus())) {
            return false;
        }
        return org.getPlanEndDate() == null || !org.getPlanEndDate().isBefore(LocalDate.now(VN_ZONE));
    }

    /** Rejects the DEMO (free upgrade) payment method when {@code demo.enabled=false} (production default). */
    public void assertDemoPaymentAllowed(String paymentMethod) {
        String method = paymentMethod == null || paymentMethod.isBlank() ? PAYMENT_METHOD_DEMO : paymentMethod;
        if (PAYMENT_METHOD_DEMO.equalsIgnoreCase(method.trim()) && !demoProperties.isEnabled()) {
            throw new BusinessRuleException(
                    "Nâng cấp demo đã bị tắt. Vui lòng thanh toán qua QR để nâng cấp Premium.");
        }
    }

    @Transactional
    public ProfileMeResponse subscribeB2c(UUID userId, String paymentMethod) {
        assertDemoPaymentAllowed(paymentMethod);
        createB2cSubscription(userId, paymentMethod);
        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return profileMeService.build(profile);
    }

    /**
     * Activates B2C Premium and returns the created subscription so callers (e.g. SePay) can link it to a payment.
     * DEMO is still blocked when demo is disabled.
     */
    @Transactional
    public B2cSubscription subscribeB2cReturningSubscription(UUID userId, String paymentMethod) {
        assertDemoPaymentAllowed(paymentMethod);
        return createB2cSubscription(userId, paymentMethod);
    }

    private B2cSubscription createB2cSubscription(UUID userId, String paymentMethod) {
        emailVerifiedGuard.assertEmailVerified(userId);
        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (profile.getOrgId() != null) {
            throw new BusinessRuleException("Bạn thuộc tổ chức — không thể đăng ký gói cá nhân B2C.");
        }

        LocalDate start = LocalDate.now(VN_ZONE);
        LocalDate end = start.plusMonths(1);
        b2cSubscriptionRepository.findByUserIdAndIsActiveTrue(userId).ifPresent(existing -> {
            existing.setIsActive(false);
            b2cSubscriptionRepository.saveAndFlush(existing);
        });
        B2cSubscription created = b2cSubscriptionRepository.save(B2cSubscription.builder()
                .userId(userId)
                .priceVnd(billingSettingsService.getB2cPremiumPriceVnd())
                .startDate(start)
                .endDate(end)
                .isActive(true)
                .paymentMethod(paymentMethod == null || paymentMethod.isBlank() ? PAYMENT_METHOD_DEMO : paymentMethod)
                .createdAt(Instant.now())
                .build());

        profile.setTier(UserTier.PREMIUM.name());
        profileRepository.save(profile);
        return created;
    }

    @Transactional
    public OrgBillingStatus subscribeOrg(UUID userId, OrgSubscribeRequest request) {
        return subscribeOrg(userId, request, "DEMO");
    }

    @Transactional
    public OrgBillingStatus subscribeOrg(UUID userId, OrgSubscribeRequest request, String paymentMethod) {
        assertDemoPaymentAllowed(paymentMethod);
        emailVerifiedGuard.assertEmailVerified(userId);
        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        OrgSubscription plan = OrgSubscription.fromPlanType(request.planType());
        if (plan == OrgSubscription.NONE) {
            throw new BusinessRuleException("planType phải là MICRO, STANDARD hoặc PREMIUM");
        }
        OrgPlanLimits.Limits limits = OrgPlanLimits.forPlan(plan);

        Organization org;
        if (request.organizationId() != null) {
            org = organizationRepository
                    .findById(request.organizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        } else if (profile.getOrgId() != null) {
            org = organizationRepository
                    .findById(profile.getOrgId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        } else {
            String name = request.orgName() == null ? "Tổ chức mới" : request.orgName().trim();
            if (name.isBlank()) {
                throw new BusinessRuleException("Tên tổ chức không được rỗng");
            }
            org = organizationRepository.save(Organization.builder()
                    .name(name)
                    .slug(slugify(name))
                    .plan(plan.name().toLowerCase())
                    .planType(plan.name())
                    .contactEmail(request.contactEmail())
                    .status(ORG_STATUS_ACTIVE)
                    .maxCcu(limits.maxCcu())
                    .maxVerifiedAccounts(limits.maxVerifiedAccounts())
                    .maxAiQueriesPerMonth(limits.maxAiQueriesPerMonth())
                    .planStartDate(LocalDate.now(VN_ZONE))
                    .planEndDate(LocalDate.now(VN_ZONE).plusYears(1))
                    .createdAt(Instant.now())
                    .build());
            organizationMemberRepository.save(OrganizationMember.builder()
                    .organizationId(org.getId())
                    .userId(userId)
                    .orgRole("teacher")
                    .invitedAt(Instant.now())
                    .acceptedAt(Instant.now())
                    .build());
            profile.setOrgId(org.getId());
            if (UserRole.fromStored(profile.getRole()) == UserRole.USER) {
                profile.setRole(UserRole.TEACHER.name());
            }
        }

        org.setPlanType(plan.name());
        org.setPlan(plan.name().toLowerCase());
        org.setContactEmail(request.contactEmail());
        org.setMaxCcu(limits.maxCcu());
        org.setMaxVerifiedAccounts(limits.maxVerifiedAccounts());
        org.setMaxAiQueriesPerMonth(limits.maxAiQueriesPerMonth());
        org.setPlanStartDate(LocalDate.now(VN_ZONE));
        org.setPlanEndDate(LocalDate.now(VN_ZONE).plusYears(1));
        org.setStatus(ORG_STATUS_ACTIVE);
        organizationRepository.save(org);

        orgBillingSubscriptionRepository.findAllByOrganizationIdOrderByCreatedAtDesc(org.getId()).stream()
                .filter(sub -> Boolean.TRUE.equals(sub.getIsActive()))
                .forEach(sub -> {
                    sub.setIsActive(false);
                    orgBillingSubscriptionRepository.save(sub);
                });

        orgBillingSubscriptionRepository.save(OrgBillingSubscription.builder()
                .organizationId(org.getId())
                .planType(plan.name())
                .priceVnd(limits.priceVnd())
                .startDate(LocalDate.now(VN_ZONE))
                .endDate(LocalDate.now(VN_ZONE).plusYears(1))
                .isActive(true)
                .paymentMethod(paymentMethod == null ? "DEMO" : paymentMethod)
                .createdAt(Instant.now())
                .build());

        profile.setOrgSubscription(plan.name());
        profile.setOrgId(org.getId());
        profileRepository.save(profile);

        return buildOrgStatus(org);
    }

    @Transactional
    public OrgBillingStatus createOrgTrial(UUID userId, OrgTrialRequest request) {
        emailVerifiedGuard.assertEmailVerified(userId);
        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (profile.getOrgId() != null) {
            throw new BusinessRuleException("Bạn đã thuộc tổ chức. Không thể tạo trial mới.");
        }
        boolean hadTrialBefore = organizationMemberRepository.findByUserId(userId).stream()
                .filter(m -> "teacher".equalsIgnoreCase(m.getOrgRole()))
                .map(m -> organizationRepository.findById(m.getOrganizationId()).orElse(null))
                .filter(org -> org != null)
                .anyMatch(org -> ORG_STATUS_TRIAL_ACTIVE.equalsIgnoreCase(org.getStatus())
                        || ORG_STATUS_TRIAL_EXPIRED.equalsIgnoreCase(org.getStatus()));
        if (hadTrialBefore) {
            throw new BusinessRuleException("Mỗi giáo viên chỉ được tạo 1 lớp học dùng thử.");
        }

        String orgName = request != null && request.orgName() != null ? request.orgName().trim() : "";
        if (orgName.isBlank()) {
            orgName = "Classroom Trial " + LocalDate.now(VN_ZONE);
        }
        String contactEmail = request != null && request.contactEmail() != null
                ? request.contactEmail().trim()
                : profile.getEmail();

        OrgPlanLimits.Limits standard = OrgPlanLimits.forPlan(OrgSubscription.STANDARD);
        Organization org = organizationRepository.save(Organization.builder()
                .name(orgName)
                .slug(slugify(orgName))
                .plan(OrgSubscription.STANDARD.name().toLowerCase())
                .planType(OrgSubscription.STANDARD.name())
                .contactEmail(contactEmail)
                .status(ORG_STATUS_TRIAL_ACTIVE)
                .maxCcu(standard.maxCcu())
                .maxVerifiedAccounts(11)
                .maxAiQueriesPerMonth(standard.maxAiQueriesPerMonth())
                .planStartDate(LocalDate.now(VN_ZONE))
                .planEndDate(LocalDate.now(VN_ZONE).plusDays(14))
                .createdAt(Instant.now())
                .build());

        organizationMemberRepository.save(OrganizationMember.builder()
                .organizationId(org.getId())
                .userId(userId)
                .orgRole("teacher")
                .invitedAt(Instant.now())
                .acceptedAt(Instant.now())
                .build());
        profile.setOrgId(org.getId());
        profile.setOrgSubscription(OrgSubscription.STANDARD.name());
        if (UserRole.fromStored(profile.getRole()) == UserRole.USER) {
            profile.setRole(UserRole.TEACHER.name());
        }
        profileRepository.save(profile);

        return buildOrgStatus(org);
    }

    @Transactional
    public int reconcileExpiredTrials() {
        LocalDate today = LocalDate.now(VN_ZONE);
        List<Organization> expiring = organizationRepository.findAll().stream()
                .filter(org -> ORG_STATUS_TRIAL_ACTIVE.equalsIgnoreCase(org.getStatus()))
                .filter(org -> org.getPlanEndDate() != null && org.getPlanEndDate().isBefore(today))
                .toList();
        for (Organization org : expiring) {
            org.setStatus(ORG_STATUS_TRIAL_EXPIRED);
            organizationRepository.save(org);
        }
        return expiring.size();
    }

    /**
     * Batch job: deactivates expired active B2C subscriptions and downgrades non-org profiles to FREE.
     *
     * @return number of subscriptions expired
     */
    @Transactional
    public int reconcileExpiredB2cSubscriptions() {
        LocalDate today = LocalDate.now(VN_ZONE);
        List<B2cSubscription> expired = b2cSubscriptionRepository.findAllByIsActiveTrueAndEndDateBefore(today);
        for (B2cSubscription sub : expired) {
            sub.setIsActive(false);
            b2cSubscriptionRepository.save(sub);
            profileRepository.findById(sub.getUserId()).ifPresent(profile -> {
                if (profile.getOrgId() == null
                        && UserTier.PREMIUM == UserTier.fromStored(profile.getTier())) {
                    profile.setTier(UserTier.FREE.name());
                    profileRepository.save(profile);
                }
            });
        }
        return expired.size();
    }

    @Transactional
    public BillingStatusResponse getStatus(UUID userId) {
        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        profile = reconcileExpiredEntitlements(profile);
        final Profile resolvedProfile = profile;
        B2cBillingStatus b2c = b2cSubscriptionRepository
                .findByUserIdAndIsActiveTrue(userId)
                .map(sub -> toB2cStatus(
                        resolvedProfile.getTier(),
                        sub.getEndDate(),
                        Boolean.TRUE.equals(sub.getIsActive()),
                        sub.getPriceVnd()))
                .orElse(toB2cStatus(
                        resolvedProfile.getTier() == null ? UserTier.FREE.name() : resolvedProfile.getTier(),
                        null,
                        false,
                        billingSettingsService.getB2cPremiumPriceVnd()));

        OrgBillingStatus orgStatus = resolvedProfile.getOrgId() == null
                ? null
                : organizationRepository
                        .findById(resolvedProfile.getOrgId())
                        .map(this::buildOrgStatus)
                        .orElse(null);

        int usedToday = usageQuotaService.getUserDailyUsage(userId);
        int dailyLimit = usageQuotaService.getUserDailyLimit(profile);
        return new BillingStatusResponse(
                profile.getTier(),
                b2c,
                orgStatus,
                usedToday,
                dailyLimit);
    }

    @Transactional
    public B2cBillingStatus getB2cStatus(UUID userId) {
        return getStatus(userId).b2c();
    }

    @Transactional
    public OrgBillingStatus getOrgStatus(UUID userId) {
        return getStatus(userId).org();
    }

    @Transactional(readOnly = true)
    public List<OrgPlanInfo> listOrgPlans() {
        return List.of(
                planInfo(OrgSubscription.MICRO, "Micro"),
                planInfo(OrgSubscription.STANDARD, "Standard"),
                planInfo(OrgSubscription.PREMIUM, "Premium"));
    }

    @Transactional(readOnly = true)
    public BillingPublicPricingResponse getPublicPricing() {
        return new BillingPublicPricingResponse(
                billingSettingsService.getB2cPremiumPriceVnd(),
                billingSettingsService.getB2cJourneyPassPriceVnd(),
                billingSettingsService.getChatFreeDailyLimit(),
                listOrgPlans());
    }

    @Transactional(readOnly = true)
    public LocalDate getUserDailyResetDate() {
        return usageQuotaService.getUserDailyResetDate();
    }

    @Transactional(readOnly = true)
    public OrgBillingStatus getOrgDashboardStats(UUID orgId) {
        Organization org = organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        return buildOrgStatus(org);
    }

    @Transactional(readOnly = true)
    public OrgBillingStatus getCurrentOrgDashboardStats(UUID userId) {
        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (profile.getOrgId() == null) {
            throw new BusinessRuleException("Bạn chưa thuộc tổ chức nào");
        }
        return getOrgDashboardStats(profile.getOrgId());
    }

    @Transactional(readOnly = true)
    public List<B2cSubscriptionHistoryItem> getB2cHistory(UUID userId) {
        return b2cSubscriptionRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(sub -> new B2cSubscriptionHistoryItem(
                        sub.getId(),
                        sub.getStartDate(),
                        sub.getEndDate(),
                        Boolean.TRUE.equals(sub.getIsActive()),
                        sub.getPriceVnd(),
                        sub.getPaymentMethod(),
                        sub.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrgSubscriptionHistoryItem> getOrgHistory(UUID userId) {
        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (profile.getOrgId() == null) {
            throw new BusinessRuleException("Bạn chưa thuộc tổ chức nào");
        }
        return orgBillingSubscriptionRepository.findAllByOrganizationIdOrderByCreatedAtDesc(profile.getOrgId()).stream()
                .map(sub -> new OrgSubscriptionHistoryItem(
                        sub.getId(),
                        sub.getPlanType(),
                        sub.getStartDate(),
                        sub.getEndDate(),
                        Boolean.TRUE.equals(sub.getIsActive()),
                        sub.getPriceVnd() == null ? 0L : sub.getPriceVnd(),
                        sub.getPaymentMethod(),
                        sub.getCreatedAt()))
                .toList();
    }

    @Transactional
    public B2cBillingStatus cancelB2c(UUID userId) {
        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        B2cSubscription active = b2cSubscriptionRepository
                .findByUserIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new BusinessRuleException("Bạn chưa có gói B2C đang hoạt động"));
        active.setIsActive(false);
        b2cSubscriptionRepository.save(active);
        if (profile.getOrgId() == null) {
            profile.setTier(UserTier.FREE.name());
            profileRepository.save(profile);
        }
        return new B2cBillingStatus(
                profile.getTier() == null ? UserTier.FREE.name() : profile.getTier(),
                active.getEndDate(),
                false,
                active.getPriceVnd(),
                computeDaysUntil(active.getEndDate()));
    }

    private B2cBillingStatus toB2cStatus(String tier, LocalDate endDate, boolean isActive, int priceVnd) {
        return new B2cBillingStatus(tier, endDate, isActive, priceVnd, computeDaysUntil(endDate));
    }

    private int computeDaysUntil(LocalDate endDate) {
        if (endDate == null) {
            return -1;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(VN_ZONE), endDate);
    }

    private OrgBillingStatus buildOrgStatus(Organization org) {
        int members = (int) organizationMemberRepository.countByOrganizationId(org.getId());
        int aiUsed = usageQuotaService.getOrgMonthlyUsage(org.getId());
        Integer aiLimit = org.getMaxAiQueriesPerMonth();
        int maxAccounts = org.getMaxVerifiedAccounts() != null ? org.getMaxVerifiedAccounts() : 100;
        int maxCcu = org.getMaxCcu() != null ? org.getMaxCcu() : 15;
        int ccuCurrent = ccuSessionService.getCurrentCcu(org.getId());
        int daysUntilExpiry = org.getPlanEndDate() == null
                ? -1
                : (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(VN_ZONE), org.getPlanEndDate());
        return new OrgBillingStatus(
                org.getId(),
                org.getName(),
                org.getPlanType() != null ? org.getPlanType() : OrgSubscription.MICRO.name(),
                org.getPlanEndDate(),
                isOrgActive(org),
                aiUsed,
                aiLimit,
                usageQuotaService.getOrgMonthlyResetDate(),
                members,
                maxAccounts,
                members >= maxAccounts,
                ccuCurrent,
                maxCcu,
                ccuCurrent >= maxCcu,
                daysUntilExpiry);
    }

    private Profile reconcileExpiredEntitlements(Profile profile) {
        reconcileExpiredB2c(profile);
        reconcileExpiredOrgMembership(profile);
        return profile;
    }

    private void reconcileExpiredB2c(Profile profile) {
        b2cSubscriptionRepository.findByUserIdAndIsActiveTrue(profile.getId()).ifPresent(active -> {
            if (active.getEndDate() != null && active.getEndDate().isBefore(LocalDate.now(VN_ZONE))) {
                active.setIsActive(false);
                b2cSubscriptionRepository.save(active);
                if (profile.getOrgId() == null) {
                    profile.setTier(UserTier.FREE.name());
                    profileRepository.save(profile);
                }
            }
        });
    }

    private void reconcileExpiredOrgMembership(Profile profile) {
        if (profile.getOrgId() == null) {
            return;
        }
        Organization org = organizationRepository.findById(profile.getOrgId()).orElse(null);
        if (org == null || isOrgActive(org)) {
            return;
        }
        if (ORG_STATUS_TRIAL_EXPIRED.equalsIgnoreCase(org.getStatus())) {
            return;
        }
        UserRole currentRole = UserRole.fromStored(profile.getRole());
        if (currentRole == UserRole.ORG_MEMBER) {
            profile.setOrgId(null);
            profile.setOrgSubscription(OrgSubscription.NONE.name());
            profile.setRole(UserRole.USER.name());
            profileRepository.save(profile);
        }
    }

    private static OrgPlanInfo planInfo(OrgSubscription plan, String label) {
        OrgPlanLimits.Limits limits = OrgPlanLimits.forPlan(plan);
        return new OrgPlanInfo(
                plan.name(),
                label,
                limits.priceVnd(),
                limits.maxCcu(),
                limits.maxVerifiedAccounts(),
                limits.maxAiQueriesPerMonth());
    }

    private static String slugify(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (base.isBlank()) {
            base = "org";
        }
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

package com.histar.be.billing.controller;

import com.histar.be.billing.entity.UsageQuota;
import com.histar.be.billing.repository.UsageQuotaRepository;
import com.histar.be.billing.service.BillingService;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.config.TestHookProperties;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/org")
@RequiredArgsConstructor
public class TestOrgQuotaController {

    public static final String TEST_HOOK_SECRET_HEADER = "X-Test-Hook-Secret";
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final TestHookProperties testHookProperties;
    private final UsageQuotaRepository usageQuotaRepository;
    private final OrganizationRepository organizationRepository;

    public record SetOrgQuotaRequest(@NotNull @Min(0) Integer usedAiQueries) {}

    @PostMapping("/{orgId}/quota")
    public ApiResponse<Void> setOrgQuota(
            @PathVariable UUID orgId,
            @RequestBody @Valid SetOrgQuotaRequest request,
            @RequestHeader(value = TEST_HOOK_SECRET_HEADER, required = false) String secret) {
        assertTestHook(secret);

        LocalDate vnDay = LocalDate.now(VN_ZONE);
        UsageQuota quota = usageQuotaRepository
                .findByOrganizationIdAndYearAndMonth(orgId, vnDay.getYear(), vnDay.getMonthValue())
                .orElseGet(() -> UsageQuota.builder()
                        .organizationId(orgId)
                        .year(vnDay.getYear())
                        .month(vnDay.getMonthValue())
                        .usedAiQueries(0)
                        .build());
        quota.setUsedAiQueries(request.usedAiQueries());
        usageQuotaRepository.save(quota);
        return ApiResponse.ok(null);
    }

    /** E2E only: expire org invite code (BR-M15 bad-path). */
    @PostMapping("/{orgId}/invite-expire")
    public ApiResponse<Void> expireInvite(
            @PathVariable UUID orgId,
            @RequestHeader(value = TEST_HOOK_SECRET_HEADER, required = false) String secret) {
        assertTestHook(secret);
        Organization org = organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        org.setInviteCodeExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        organizationRepository.save(org);
        return ApiResponse.ok(null);
    }

    /** E2E/API: expire classroom trial mid-session (CAP-TIME-01). */
    @PostMapping("/{orgId}/expire-trial")
    public ApiResponse<Void> expireTrial(
            @PathVariable UUID orgId,
            @RequestHeader(value = TEST_HOOK_SECRET_HEADER, required = false) String secret) {
        assertTestHook(secret);
        Organization org = organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        org.setStatus(BillingService.ORG_STATUS_TRIAL_EXPIRED);
        org.setPlanEndDate(LocalDate.now(VN_ZONE).minusDays(1));
        organizationRepository.save(org);
        return ApiResponse.ok(null);
    }

    private void assertTestHook(String secret) {
        if (!testHookProperties.isEnabled()) {
            throw new ResourceNotFoundException("Not found");
        }
        if (testHookProperties.getSecret() == null
                || testHookProperties.getSecret().isBlank()
                || secret == null
                || !testHookProperties.getSecret().equals(secret)) {
            throw new AuthException("Invalid test hook secret");
        }
    }
}

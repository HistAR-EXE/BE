package com.histar.be.organization.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.config.HistarAppProperties;
import com.histar.be.config.HistarOrgProperties;
import com.histar.be.organization.dto.OrgInviteCodeResponse;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.billing.service.BillingService;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrgInviteService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrganizationRepository organizationRepository;
    private final OrgAccessService orgAccessService;
    private final HistarOrgProperties histarOrgProperties;
    private final HistarAppProperties histarAppProperties;

    @Transactional(readOnly = true)
    public OrgInviteCodeResponse getInviteCode(UUID orgId) {
        orgAccessService.requireOrgAccess(orgId);
        Organization org = organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        return new OrgInviteCodeResponse(org.getInviteCode(), org.getInviteCodeExpiresAt(), org.getId(), buildInviteUrl(org.getInviteCode()));
    }

    @Transactional
    public OrgInviteCodeResponse generateInviteCode(UUID orgId) {
        orgAccessService.requireOrgAccess(orgId);
        Organization org = organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        if (BillingService.ORG_STATUS_TRIAL_EXPIRED.equalsIgnoreCase(org.getStatus())
                || BillingService.ORG_STATUS_EXPIRED.equalsIgnoreCase(org.getStatus())) {
            throw new BusinessRuleException("Tổ chức đã hết hạn. Không thể tạo mã mời mới.");
        }
        String code = generateUniqueCode();
        Instant expiresAt =
                Instant.now().plus(histarOrgProperties.getOrg().getInviteTtlDays(), ChronoUnit.DAYS);
        org.setInviteCode(code);
        org.setInviteCodeExpiresAt(expiresAt);
        organizationRepository.save(org);
        return new OrgInviteCodeResponse(code, expiresAt, org.getId(), buildInviteUrl(code));
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = randomCode(6);
            if (organizationRepository.findByInviteCodeIgnoreCase(code).isEmpty()) {
                return code;
            }
        }
        throw new BusinessRuleException("Không thể tạo mã mời, vui lòng thử lại");
    }

    private static String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private String buildInviteUrl(String code) {
        if (code == null || code.isBlank()) {
            return histarAppProperties.getFrontendUrl().replaceAll("/$", "") + "/join";
        }
        return histarAppProperties.getFrontendUrl().replaceAll("/$", "") + "/join?code=" + code;
    }
}

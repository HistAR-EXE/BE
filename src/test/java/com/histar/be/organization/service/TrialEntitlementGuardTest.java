package com.histar.be.organization.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.histar.be.billing.service.BillingService;
import com.histar.be.common.exception.TrialExpiredException;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.entity.OrganizationMember;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrialEntitlementGuardTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @InjectMocks
    private TrialEntitlementGuard trialEntitlementGuard;

    @Test
    void studentWrite_blockedWhenTrialExpired() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).orgId(orgId).build()));
        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(Organization.builder()
                        .id(orgId)
                        .status(BillingService.ORG_STATUS_TRIAL_EXPIRED)
                        .build()));
        when(organizationMemberRepository.findByOrganizationIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(OrganizationMember.builder()
                        .organizationId(orgId)
                        .userId(userId)
                        .orgRole("student")
                        .build()));

        assertThatThrownBy(() -> trialEntitlementGuard.assertStudentCanWrite(userId))
                .isInstanceOf(TrialExpiredException.class)
                .hasMessageContaining("hết hạn");
    }

    @Test
    void teacherWrite_allowedWhenTrialExpired() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).orgId(orgId).build()));
        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(Organization.builder()
                        .id(orgId)
                        .status(BillingService.ORG_STATUS_TRIAL_EXPIRED)
                        .build()));
        when(organizationMemberRepository.findByOrganizationIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(OrganizationMember.builder()
                        .organizationId(orgId)
                        .userId(userId)
                        .orgRole("teacher")
                        .build()));

        assertThatCode(() -> trialEntitlementGuard.assertStudentCanWrite(userId)).doesNotThrowAnyException();
    }

    @Test
    void studentWrite_allowedWhenTrialActive() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).orgId(orgId).build()));
        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(Organization.builder()
                        .id(orgId)
                        .status(BillingService.ORG_STATUS_TRIAL_ACTIVE)
                        .build()));

        assertThatCode(() -> trialEntitlementGuard.assertStudentCanWrite(userId)).doesNotThrowAnyException();
    }
}

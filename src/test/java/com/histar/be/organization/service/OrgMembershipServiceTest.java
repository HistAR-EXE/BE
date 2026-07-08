package com.histar.be.organization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.histar.be.billing.service.BillingService;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ErrorCode;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.entity.OrganizationMember;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrgMembershipServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private OrgMembershipService orgMembershipService;

    @Test
    void joinOrg_throwsWhenOrgAtMaxVerifiedAccounts() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Organization org = Organization.builder()
                .id(orgId)
                .inviteCode("ABC123")
                .maxVerifiedAccounts(2)
                .inviteCodeExpiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(organizationRepository.findByInviteCodeIgnoreCase("ABC123")).thenReturn(Optional.of(org));
        when(organizationRepository.findByIdForUpdate(orgId)).thenReturn(Optional.of(org));
        when(organizationMemberRepository.countByOrganizationId(orgId)).thenReturn(2L);
        when(organizationMemberRepository.findByOrganizationIdAndUserId(orgId, userId))
                .thenReturn(Optional.empty());
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).role(UserRole.USER.name()).build()));

        assertThatThrownBy(() -> orgMembershipService.joinOrg(userId, "ABC123"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("giới hạn");
    }

    @Test
    void joinOrg_allowsExistingMemberToRejoin() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Organization org = Organization.builder()
                .id(orgId)
                .name("Test School")
                .inviteCode("ABC123")
                .maxVerifiedAccounts(1)
                .inviteCodeExpiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(organizationRepository.findByInviteCodeIgnoreCase("ABC123")).thenReturn(Optional.of(org));
        when(organizationRepository.findByIdForUpdate(orgId)).thenReturn(Optional.of(org));
        when(organizationMemberRepository.findByOrganizationIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(OrganizationMember.builder()
                        .organizationId(orgId)
                        .userId(userId)
                        .orgRole("student")
                        .build()));
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder()
                        .id(userId)
                        .orgId(orgId)
                        .role(UserRole.ORG_MEMBER.name())
                        .build()));
        when(organizationMemberRepository.save(any(OrganizationMember.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        orgMembershipService.joinOrg(userId, "ABC123");
    }

    @Test
    void joinOrg_rejectsWhenOrgTrialExpired() {
        // Mockito stubs only — isolated org id, no shared DB / no cross-test leak
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Organization org = Organization.builder()
                .id(orgId)
                .inviteCode("EXP999")
                .status(BillingService.ORG_STATUS_TRIAL_EXPIRED)
                .inviteCodeExpiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(organizationRepository.findByInviteCodeIgnoreCase("EXP999")).thenReturn(Optional.of(org));

        assertThatThrownBy(() -> orgMembershipService.joinOrg(userId, "EXP999"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException bre = (BusinessRuleException) ex;
                    assertThat(bre.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_RULE);
                    assertThat(bre.getMessage()).contains("Tổ chức đã hết hạn");
                });
    }

    @Test
    void joinOrg_rejectsWhenOrgExpired() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Organization org = Organization.builder()
                .id(orgId)
                .inviteCode("END888")
                .status(BillingService.ORG_STATUS_EXPIRED)
                .inviteCodeExpiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(organizationRepository.findByInviteCodeIgnoreCase("END888")).thenReturn(Optional.of(org));

        assertThatThrownBy(() -> orgMembershipService.joinOrg(userId, "END888"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("nâng cấp");
    }
}

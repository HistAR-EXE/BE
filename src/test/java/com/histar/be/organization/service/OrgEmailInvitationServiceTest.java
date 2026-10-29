package com.histar.be.organization.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.organization.entity.OrgEmailInvitation;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrgEmailInvitationRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrgEmailInvitationServiceTest {
    @Mock OrgEmailInvitationRepository invitations;
    @Mock OrganizationRepository organizations;
    @Mock OrgMembershipService memberships;
    @Mock com.histar.be.profile.repository.ProfileRepository profiles;
    @Mock com.histar.be.organization.repository.OrganizationMemberRepository members;
    @Mock jakarta.persistence.EntityManager entityManager;
    @Mock com.histar.be.billing.service.OrgPendingSeatLimitService pendingSeats;
    OrgEmailInvitationService service;
    final UUID orgId = UUID.randomUUID();
    Profile profile;
    OrgEmailInvitation invite;

    @BeforeEach void setup() {
        lenient().when(pendingSeats.pendingLimit(any())).thenReturn(null);
        service = new OrgEmailInvitationService(invitations, organizations, memberships, profiles, members, entityManager, pendingSeats);
        profile = Profile.builder().id(UUID.randomUUID()).email("student@example.test")
                .emailVerified(true).tier("FREE").build();
        invite = OrgEmailInvitation.builder().id(UUID.randomUUID()).organizationId(orgId)
                .email(profile.getEmail()).status("PENDING").expiresAt(Instant.now().plusSeconds(3600)).build();
        lenient().when(profiles.findByIdForUpdate(profile.getId())).thenReturn(Optional.of(profile));
    }

    @Test void unverifiedEmailCannotAcceptAnInvitation() {
        profile.setEmailVerified(false);
        service.acceptPending(profile);
        verifyNoInteractions(invitations, memberships);
    }

    @Test void verifiedLoginEnrollsOnceWithoutOverwritingPersonalTier() {
        profile.setTier("PREMIUM");
        when(invitations.findPendingForEmail(profile.getEmail())).thenReturn(List.of(invite));
        var org = Organization.builder().id(orgId).status("ACTIVE").planType("MICRO").build();
        when(organizations.findById(orgId)).thenReturn(Optional.of(org));
        when(organizations.findByIdForUpdate(orgId)).thenReturn(Optional.of(org));
        service.acceptPending(profile);
        verify(memberships).enrollUserInOrganization(profile.getId(), orgId);
        assertThat(invite.getStatus()).isEqualTo("ACCEPTED");
        assertThat(profile.getOrgId()).isEqualTo(orgId);
        assertThat(profile.getOrgSubscription()).isEqualTo("MICRO");
        assertThat(profile.getTier()).isEqualTo("PREMIUM");
    }

    @Test void expiredInvitationDoesNotGrantAccess() {
        invite.setExpiresAt(Instant.now().minusSeconds(1));
        when(invitations.findPendingForEmail(profile.getEmail())).thenReturn(List.of(invite));
        service.acceptPending(profile);
        verifyNoInteractions(memberships);
        assertThat(invite.getStatus()).isEqualTo("PENDING");
    }

    @Test void anotherOrganizationCannotTakeOverExistingMember() {
        profile.setOrgId(UUID.randomUUID());
        when(invitations.findPendingForEmail(profile.getEmail())).thenReturn(List.of(invite));
        service.acceptPending(profile);
        verifyNoInteractions(memberships);
        assertThat(invite.getStatus()).isEqualTo("PENDING");
    }

    @Test void fullOrganizationLeavesInvitePendingWithoutBlockingLogin() {
        when(invitations.findPendingForEmail(profile.getEmail())).thenReturn(List.of(invite));
        var org = Organization.builder().id(orgId).status("ACTIVE").planType("STANDARD").build();
        when(organizations.findById(orgId)).thenReturn(Optional.of(org));
        when(organizations.findByIdForUpdate(orgId)).thenReturn(Optional.of(org));
        when(members.countByOrganizationId(orgId)).thenReturn(100L);
        service.acceptPending(profile);
        verifyNoInteractions(memberships);
        assertThat(invite.getStatus()).isEqualTo("PENDING");
    }

    @Test void expiredOrganizationCannotGrantPremium() {
        when(invitations.findPendingForEmail(profile.getEmail())).thenReturn(List.of(invite));
        when(organizations.findById(orgId)).thenReturn(Optional.of(Organization.builder().id(orgId)
                .status("EXPIRED").build()));
        service.acceptPending(profile);
        verifyNoInteractions(memberships);
    }

    @Test void multipleInvitationsDoNotPreventAuthenticationOrChooseOrganizationSilently() {
        UUID otherOrg = UUID.randomUUID();
        var second = OrgEmailInvitation.builder().organizationId(otherOrg).email(profile.getEmail())
                .status("PENDING").expiresAt(Instant.now().plusSeconds(3600)).build();
        when(invitations.findPendingForEmail(profile.getEmail())).thenReturn(List.of(invite, second));
        when(organizations.findById(orgId)).thenReturn(Optional.of(Organization.builder().id(orgId).status("ACTIVE").build()));
        when(organizations.findById(otherOrg)).thenReturn(Optional.of(Organization.builder().id(otherOrg).status("ACTIVE").build()));
        service.acceptPending(profile);
        verifyNoInteractions(memberships);
        assertThat(profile.getOrgId()).isNull();
    }
}

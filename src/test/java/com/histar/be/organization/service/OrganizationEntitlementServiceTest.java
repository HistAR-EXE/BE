package com.histar.be.organization.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.entity.OrganizationMember;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrganizationEntitlementServiceTest {
    private final OrganizationRepository organizations = mock(OrganizationRepository.class);
    private final OrganizationMemberRepository members = mock(OrganizationMemberRepository.class);
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final OrganizationEntitlementService service = new OrganizationEntitlementService(organizations, members, profiles);
    private final UUID orgId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private void setup(String plan, String role) {
        when(organizations.findById(orgId)).thenReturn(Optional.of(Organization.builder().id(orgId)
                .status("ACTIVE").planType(plan).planEndDate(LocalDate.now().plusDays(30)).build()));
        when(profiles.findById(userId)).thenReturn(Optional.of(Profile.builder().id(userId).orgId(orgId).emailVerified(true).build()));
        when(members.findByOrganizationIdAndUserId(orgId, userId)).thenReturn(Optional.of(OrganizationMember.builder()
                .userId(userId).organizationId(orgId).orgRole(role).acceptedAt(Instant.now()).build()));
    }

    @Test void microOwnerCanManageTeachersButCannotAssignOrSeeLiveBoard() {
        setup("MICRO", "owner");
        var access = service.capabilities(userId, orgId);
        assertThat(access.manageTeachers()).isTrue();
        assertThat(access.manageMembers()).isTrue();
        assertThat(access.premiumAccess()).isTrue();
        assertThat(access.assignments()).isFalse();
        assertThat(access.liveBoard()).isFalse();
        assertThat(access.reports()).isFalse();
    }

    @Test void standardTeacherCanAssignAndReportButCannotProvisionTeachersOrUsePremiumTrips() {
        setup("STANDARD", "teacher");
        var access = service.capabilities(userId, orgId);
        assertThat(access.assignments()).isTrue();
        assertThat(access.liveBoard()).isTrue();
        assertThat(access.reports()).isTrue();
        assertThat(access.manageTeachers()).isFalse();
        assertThat(access.fieldTrips()).isFalse();
        assertThat(access.contentDrafts()).isFalse();
    }

    @Test void studentCannotUseTeacherFunctions() {
        setup("PREMIUM", "student");
        var access = service.capabilities(userId, orgId);
        assertThat(access.premiumAccess()).isTrue();
        assertThat(access.assignments()).isFalse();
        assertThat(access.manageMembers()).isFalse();
    }

    @Test void expiredOrganizationRevokesAllOrganizationCapabilities() {
        setup("PREMIUM", "owner");
        var org = organizations.findById(orgId).orElseThrow();
        org.setPlanEndDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1));
        var access = service.capabilities(userId, orgId);
        assertThat(access.organizationActive()).isFalse();
        assertThat(access.premiumAccess()).isFalse();
        assertThat(access.manageTeachers()).isFalse();
        assertThat(access.assignments()).isFalse();
    }

    @Test void foreignOrganizationAndUnverifiedProfileCannotAcquireAccess() {
        setup("STANDARD", "teacher");
        profiles.findById(userId).orElseThrow().setEmailVerified(false);
        assertThat(service.hasActiveMembership(profiles.findById(userId).orElseThrow())).isFalse();
        assertThatThrownBy(() -> service.capabilities(userId, UUID.randomUUID())).isInstanceOf(RuntimeException.class);
    }
}

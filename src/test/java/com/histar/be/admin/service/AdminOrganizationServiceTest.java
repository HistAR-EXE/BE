package com.histar.be.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.admin.dto.CreateAdminOrganizationRequest;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.config.HistarAppProperties;
import com.histar.be.config.HistarMailProperties;
import com.histar.be.mail.HistarEmailService;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.organization.service.OrgInviteService;
import com.histar.be.organization.service.OrgMembershipService;
import com.histar.be.organization.service.OrganizationAnalyticsService;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminOrganizationServiceTest {

    @Mock private OrganizationRepository organizations;
    @Mock private OrganizationMemberRepository members;
    @Mock private OrganizationAnalyticsService analytics;
    @Mock private OrgInviteService inviteService;
    @Mock private OrgMembershipService membershipService;
    @Mock private ProfileRepository profiles;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private HistarEmailService emailService;
    @Mock private HistarAppProperties appProperties;
    @Mock private HistarMailProperties mailProperties;
    @Mock private AdminAuditService audit;
    @Mock private CurrentUserAccessor currentUser;

    private AdminOrganizationService service;

    @BeforeEach
    void setUp() {
        service = new AdminOrganizationService(
                organizations,
                members,
                analytics,
                inviteService,
                membershipService,
                profiles,
                passwordEncoder,
                emailService,
                appProperties,
                mailProperties,
                audit,
                currentUser);
    }

    @Test
    void listsOrganizationsWithFiltersServerPaginationAndNullableOrgType() {
        UUID id = UUID.randomUUID();
        Organization organization = Organization.builder()
                .id(id)
                .name("FPT School")
                .slug("fpt-school")
                .plan("FREE")
                .planType("SCHOOL_PRO")
                .status("ACTIVE")
                .build();
        when(organizations.findAdminOrganizations(
                        eq("fpt"), eq("SCHOOL_PRO"), eq("ACTIVE"), any()))
                .thenReturn(new PageImpl<>(List.of(organization), PageRequest.of(2, 10), 30));
        when(members.countByOrganizationId(id)).thenReturn(42L);

        var response = service.list(2, 10, " fpt ", "school_pro", "active");

        assertThat(response.getItems()).singleElement().satisfies(item -> {
            assertThat(item.name()).isEqualTo("FPT School");
            assertThat(item.orgType()).isNull();
            assertThat(item.memberCount()).isEqualTo(42);
        });
        assertThat(response.getTotalItems()).isEqualTo(30);
        assertThat(response.getTotalPages()).isEqualTo(3);
        verify(organizations).findAdminOrganizations(
                eq("fpt"), eq("SCHOOL_PRO"), eq("ACTIVE"),
                eq(PageRequest.of(2, 10, Sort.by(Sort.Direction.ASC, "name").and(Sort.by("id")))));
    }

    @Test
    void createsOrganizationWithDefaultsAndAudit() {
        UUID actor = UUID.randomUUID();
        when(currentUser.getUserId()).thenReturn(Optional.of(actor));
        when(organizations.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization org = invocation.getArgument(0);
            org.setId(UUID.randomUUID());
            return org;
        });
        when(members.countByOrganizationId(any())).thenReturn(0L);

        var created = service.create(new CreateAdminOrganizationRequest(
                "Trường Demo HistAR", "STANDARD", "contact@demo.vn", null));

        assertThat(created.name()).isEqualTo("Trường Demo HistAR");
        assertThat(created.planType()).isEqualTo("STANDARD");
        assertThat(created.status()).isEqualTo("ACTIVE");
        ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
        verify(organizations).save(captor.capture());
        assertThat(captor.getValue().getContactEmail()).isEqualTo("contact@demo.vn");
        verify(audit).append(any());
    }
}

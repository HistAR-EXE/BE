package com.histar.be.organization.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.HistarAppProperties;
import com.histar.be.config.HistarMailProperties;
import com.histar.be.organization.dto.ProvisionStudentRequest;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.histar.be.mail.HistarEmailService;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class OrgStudentProvisionServiceTest {

    @Mock
    private OrgAccessService orgAccessService;

    @Mock
    private OrgMembershipService orgMembershipService;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HistarEmailService histarEmailService;

    @Mock
    private HistarAppProperties appProperties;

    @Mock
    private HistarMailProperties mailProperties;

    @InjectMocks
    private OrgStudentProvisionService orgStudentProvisionService;

    private UUID orgId;
    private UUID teacherId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
    }

    @Test
    void provisionStudent_throwsWhenOrgAtMaxVerifiedAccounts() {
        Organization org = Organization.builder()
                .id(orgId)
                .name("Full School")
                .maxVerifiedAccounts(2)
                .build();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(profileRepository.findByEmail("new@school.vn")).thenReturn(Optional.empty());
        when(profileRepository.findById(teacherId))
                .thenReturn(Optional.of(com.histar.be.profile.entity.Profile.builder()
                        .id(teacherId)
                        .displayName("Teacher")
                        .build()));
        doThrow(new BusinessRuleException("Tổ chức đã đạt giới hạn 2 tài khoản. Liên hệ giáo viên để nâng gói."))
                .when(orgMembershipService)
                .assertOrgHasSeat(orgId);

        assertThatThrownBy(() -> orgStudentProvisionService.provisionStudent(
                        orgId,
                        teacherId,
                        new ProvisionStudentRequest("new@school.vn", "Student", false)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("giới hạn");
    }
}

package com.histar.be.organization.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.organization.dto.ProvisionStudentRequest;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrgTeacherProvisionServiceTest {
    @Test void regularTeacherCannotProvisionAnotherTeacher() {
        var entitlements = mock(OrganizationEntitlementService.class);
        var students = mock(OrgStudentProvisionService.class);
        var profiles = mock(ProfileRepository.class);
        var members = mock(OrganizationMemberRepository.class);
        var service = new OrgTeacherProvisionService(entitlements, students, profiles, members);
        UUID owner = UUID.randomUUID(), org = UUID.randomUUID();
        doThrow(new BusinessRuleException("Chỉ chủ tổ chức được cấp giáo viên"))
                .when(entitlements).requireCapability(owner, org, "manageTeachers");
        assertThatThrownBy(() -> service.provision(org, owner,
                new ProvisionStudentRequest("teacher@example.test", "Teacher", false)))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(students, profiles, members);
    }
}

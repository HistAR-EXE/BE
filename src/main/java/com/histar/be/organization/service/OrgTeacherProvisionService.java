package com.histar.be.organization.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.organization.dto.ProvisionStudentRequest;
import com.histar.be.organization.dto.ProvisionStudentResponse;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class OrgTeacherProvisionService {
    private final OrganizationEntitlementService entitlements;
    private final OrgStudentProvisionService students;
    private final ProfileRepository profiles;
    private final OrganizationMemberRepository members;

    @Transactional
    public ProvisionStudentResponse provision(UUID orgId, UUID actorId, ProvisionStudentRequest request) {
        entitlements.requireCapability(actorId, orgId, "manageTeachers");
        profiles.findByEmail(request.email().trim().toLowerCase(Locale.ROOT)).ifPresent(existing -> {
            if ("ADMIN".equalsIgnoreCase(existing.getRole()) || existing.getId().equals(actorId)) {
                throw new BusinessRuleException("Không thể thay đổi vai trò quản trị bằng thao tác cấp giáo viên");
            }
            members.findByOrganizationIdAndUserId(orgId, existing.getId()).ifPresent(member -> {
                if ("owner".equalsIgnoreCase(member.getOrgRole())) {
                    throw new BusinessRuleException("Không thể thay đổi chủ tổ chức bằng thao tác cấp giáo viên");
                }
            });
        });
        // The owner receives credentials locally; never silently emails a temporary password.
        var result = students.provisionStudent(orgId, actorId,
                new ProvisionStudentRequest(request.email(), request.displayName(), false));
        var profile = profiles.findById(result.userId()).orElseThrow();
        var member = members.findByOrganizationIdAndUserId(orgId, profile.getId()).orElseThrow();
        profile.setRole("TEACHER");
        member.setOrgRole("teacher");
        profiles.save(profile);
        members.save(member);
        return result;
    }
}

package com.histar.be.lms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ErrorCode;
import com.histar.be.common.exception.LmsPremiumRequiredException;
import com.histar.be.lms.dto.CreateAssignmentRequest;
import com.histar.be.lms.entity.Assignment;
import com.histar.be.lms.repository.AssignmentRepository;
import com.histar.be.lms.repository.AssignmentSubmissionRepository;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.entity.OrganizationMember;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.userquestprogress.entity.UserQuestProgress;
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LmsAssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private QuestRepository questRepository;

    @Mock
    private UserQuestProgressRepository userQuestProgressRepository;

    @InjectMocks
    private LmsAssignmentService lmsAssignmentService;

    @Test
    void listForTeacher_throwsWhenMemberRoleIsNotTeacher() {
        UUID teacherId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Profile profile = Profile.builder().id(teacherId).orgId(orgId).build();
        when(profileRepository.findById(teacherId)).thenReturn(Optional.of(profile));
        when(organizationMemberRepository.findByOrganizationIdAndUserId(orgId, teacherId))
                .thenReturn(Optional.of(OrganizationMember.builder()
                        .organizationId(orgId)
                        .userId(teacherId)
                        .orgRole("student")
                        .build()));

        assertThatThrownBy(() -> lmsAssignmentService.listForTeacher(teacherId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Chỉ giáo viên");
    }

    @Test
    void autoGradeOnQuestCompletion_onlyGradesAssignmentsInStudentOrg() {
        UUID studentId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();

        when(profileRepository.findById(studentId))
                .thenReturn(Optional.of(Profile.builder().id(studentId).orgId(orgId).build()));
        when(organizationMemberRepository.findByOrganizationIdAndUserId(orgId, studentId))
                .thenReturn(Optional.of(OrganizationMember.builder()
                        .organizationId(orgId)
                        .userId(studentId)
                        .orgRole("student")
                        .build()));
        when(assignmentRepository.findByOrgIdOrderByCreatedAtDesc(orgId))
                .thenReturn(List.of(Assignment.builder()
                        .id(assignmentId)
                        .orgId(orgId)
                        .questId(questId)
                        .createdAt(Instant.now())
                        .build()));
        when(userQuestProgressRepository.findByUserIdAndQuestId(studentId, questId))
                .thenReturn(Optional.of(UserQuestProgress.builder().status("completed").build()));
        when(assignmentSubmissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId))
                .thenReturn(Optional.empty());

        lmsAssignmentService.autoGradeOnQuestCompletion(studentId, questId);

        verify(assignmentRepository).findByOrgIdOrderByCreatedAtDesc(orgId);
        verify(assignmentSubmissionRepository).save(any());
    }

    @Test
    void autoGradeOnQuestCompletion_skipsWhenMemberIsNotStudent() {
        UUID studentId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();

        when(profileRepository.findById(studentId))
                .thenReturn(Optional.of(Profile.builder().id(studentId).orgId(orgId).build()));
        when(organizationMemberRepository.findByOrganizationIdAndUserId(orgId, studentId))
                .thenReturn(Optional.of(OrganizationMember.builder()
                        .organizationId(orgId)
                        .userId(studentId)
                        .orgRole("teacher")
                        .build()));

        lmsAssignmentService.autoGradeOnQuestCompletion(studentId, questId);

        verify(assignmentRepository, never()).findByOrgIdOrderByCreatedAtDesc(any());
        verify(assignmentSubmissionRepository, never()).save(any());
    }

    @Test
    void listForTeacher_allowsTeacherInPremiumOrg() {
        UUID teacherId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Profile profile = Profile.builder().id(teacherId).orgId(orgId).build();
        when(profileRepository.findById(teacherId)).thenReturn(Optional.of(profile));
        when(organizationMemberRepository.findByOrganizationIdAndUserId(orgId, teacherId))
                .thenReturn(Optional.of(OrganizationMember.builder()
                        .organizationId(orgId)
                        .userId(teacherId)
                        .orgRole("teacher")
                        .build()));
        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(Organization.builder().id(orgId).planType("PREMIUM").build()));
        when(assignmentRepository.findByOrgIdOrderByCreatedAtDesc(orgId)).thenReturn(List.of());

        lmsAssignmentService.listForTeacher(teacherId);

        verify(assignmentRepository).findByOrgIdOrderByCreatedAtDesc(orgId);
    }

    @Test
    void createAssignment_rejectsStandardTierWithLmsPremiumRequiredCode() {
        UUID teacherId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        Profile profile = Profile.builder().id(teacherId).orgId(orgId).build();
        when(profileRepository.findById(teacherId)).thenReturn(Optional.of(profile));
        when(organizationMemberRepository.findByOrganizationIdAndUserId(orgId, teacherId))
                .thenReturn(Optional.of(OrganizationMember.builder()
                        .organizationId(orgId)
                        .userId(teacherId)
                        .orgRole("teacher")
                        .build()));
        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(Organization.builder().id(orgId).planType("STANDARD").build()));

        assertThatThrownBy(() ->
                        lmsAssignmentService.createAssignment(
                                teacherId, new CreateAssignmentRequest("Bai tap 1", questId, null)))
                .isInstanceOf(LmsPremiumRequiredException.class)
                .satisfies(ex -> {
                    LmsPremiumRequiredException lmsEx = (LmsPremiumRequiredException) ex;
                    assertThat(lmsEx.getErrorCode()).isEqualTo(ErrorCode.LMS_PREMIUM_REQUIRED);
                    assertThat(lmsEx.getMessage()).contains("Premium B2B");
                });
        verify(assignmentRepository, never()).save(any());
        verify(questRepository, never()).findById(any());
    }
}

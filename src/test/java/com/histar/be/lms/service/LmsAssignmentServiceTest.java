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
import com.histar.be.fieldtrip.entity.FieldTrip;
import com.histar.be.fieldtrip.entity.FieldTripStatus;
import com.histar.be.fieldtrip.entity.SiteConfirmationStatus;
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
import com.histar.be.quest.campaign.QuestCampaignAdminService;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.campaign.QuestCampaignRepository;
import com.histar.be.quest.progress.entity.UserQuestProgress;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    @Mock
    private QuestCampaignRepository campaignRepository;

    @Mock
    private QuestCampaignAdminService campaignAdminService;

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
                                teacherId, new CreateAssignmentRequest("Bai tap 1", questId, null, null)))
                .isInstanceOf(LmsPremiumRequiredException.class)
                .satisfies(ex -> {
                    LmsPremiumRequiredException lmsEx = (LmsPremiumRequiredException) ex;
                    assertThat(lmsEx.getErrorCode()).isEqualTo(ErrorCode.LMS_PREMIUM_REQUIRED);
                    assertThat(lmsEx.getMessage()).contains("Premium B2B");
                });
        verify(assignmentRepository, never()).save(any());
        verify(questRepository, never()).findById(any());
    }

    @Test
    void ensureForFieldTrip_returnsExistingAssignmentWithoutSavingAgain() {
        FieldTrip trip = fieldTrip();
        Assignment existing = Assignment.builder()
                .id(UUID.randomUUID())
                .orgId(trip.getOrgId())
                .teacherId(trip.getCreatedBy())
                .questId(trip.getQuestId())
                .campaignId(trip.getCampaignId())
                .fieldTripId(trip.getId())
                .title("Đã tạo")
                .build();
        when(assignmentRepository.findByFieldTripId(trip.getId())).thenReturn(Optional.of(existing));

        Assignment result = lmsAssignmentService.ensureForFieldTrip(trip);

        assertThat(result).isSameAs(existing);
        verify(assignmentRepository, never()).save(any());
        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void ensureForFieldTrip_createsOneAssignmentTiedToTheTrip() {
        FieldTrip trip = fieldTrip();
        when(assignmentRepository.findByFieldTripId(trip.getId())).thenReturn(Optional.empty());
        when(assignmentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Assignment result = lmsAssignmentService.ensureForFieldTrip(trip);

        assertThat(result.getFieldTripId()).isEqualTo(trip.getId());
        assertThat(result.getOrgId()).isEqualTo(trip.getOrgId());
        assertThat(result.getTeacherId()).isEqualTo(trip.getCreatedBy());
        assertThat(result.getQuestId()).isEqualTo(trip.getQuestId());
        assertThat(result.getCampaignId()).isEqualTo(trip.getCampaignId());
        assertThat(result.getTitle()).isEqualTo(trip.getTitle());
        assertThat(result.getDueAt()).isEqualTo(trip.getVisitEnd());
        verify(assignmentRepository).saveAndFlush(any());
    }

    @Test
    void ensureForFieldTrip_readsWinnerInCallerTransactionAfterInsertConflict() {
        FieldTrip trip = fieldTrip();
        Assignment winner = Assignment.builder()
                .id(UUID.randomUUID())
                .fieldTripId(trip.getId())
                .questId(trip.getQuestId())
                .campaignId(trip.getCampaignId())
                .title(trip.getTitle())
                .build();
        AtomicReference<String> failedInsert = new AtomicReference<>();
        AtomicInteger reads = new AtomicInteger();
        when(assignmentRepository.findByFieldTripId(trip.getId())).thenAnswer(invocation -> {
            String reader = assignmentWriter();
            if (reader.equals(failedInsert.get())) {
                throw new IllegalStateException(
                        "current transaction is aborted, commands ignored until end of transaction block");
            }
            return reads.incrementAndGet() == 1 ? Optional.empty() : Optional.of(winner);
        });
        when(assignmentRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            String writer = assignmentWriter();
            assertThat(isRequiresNew(writer))
                    .as("insert must roll back alone so the caller can read the committed winner")
                    .isTrue();
            failedInsert.set(writer);
            throw new DataIntegrityViolationException("duplicate key uq_assignments_field_trip_id");
        });

        Assignment result = lmsAssignmentService.ensureForFieldTrip(trip);

        assertThat(result).isSameAs(winner);
        assertThat(reads.get()).isEqualTo(2);
        verify(assignmentRepository).saveAndFlush(any());
    }

    private static String assignmentWriter() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                .filter(frame -> frame.getClassName().equals(LmsAssignmentService.class.getName()))
                .map(StackTraceElement::getMethodName)
                .findFirst()
                .orElse("");
    }

    private static boolean isRequiresNew(String methodName) {
        return Arrays.stream(LmsAssignmentService.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .map(method -> method.getAnnotation(Transactional.class))
                .anyMatch(transactional ->
                        transactional != null && transactional.propagation() == Propagation.REQUIRES_NEW);
    }

    private FieldTrip fieldTrip() {
        return FieldTrip.builder()
                .id(UUID.randomUUID())
                .orgId(UUID.randomUUID())
                .siteCode("cu-chi")
                .title("Chuyến lớp 6")
                .visitStart(Instant.parse("2026-11-01T01:00:00Z"))
                .visitEnd(Instant.parse("2026-11-01T06:00:00Z"))
                .questId(UUID.randomUUID())
                .campaignId(UUID.randomUUID())
                .joinCode("ABCDEFGH")
                .status(FieldTripStatus.DRAFT)
                .siteConfirmationStatus(SiteConfirmationStatus.NOT_REQUIRED)
                .createdBy(UUID.randomUUID())
                .createdAt(Instant.parse("2026-10-24T00:00:00Z"))
                .updatedAt(Instant.parse("2026-10-24T00:00:00Z"))
                .rowVersion(0L)
                .build();
    }
}

package com.histar.be.lms.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.LmsPremiumRequiredException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.lms.dto.AssignmentResponse;
import com.histar.be.lms.dto.AssignmentSubmissionResponse;
import com.histar.be.lms.dto.CreateAssignmentRequest;
import com.histar.be.lms.entity.Assignment;
import com.histar.be.lms.entity.AssignmentSubmission;
import com.histar.be.lms.repository.AssignmentRepository;
import com.histar.be.lms.repository.AssignmentSubmissionRepository;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.entity.OrganizationMember;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.userquestprogress.entity.UserQuestProgress;
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LmsAssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final ProfileRepository profileRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final QuestRepository questRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;

    @Transactional
    public AssignmentResponse createAssignment(UUID teacherId, CreateAssignmentRequest request) {
        Profile teacher = profileRepository
                .findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        assertPremiumLms(teacher);
        Quest quest = questRepository
                .findById(request.questId())
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found"));
        Instant now = Instant.now();
        Assignment saved = assignmentRepository.save(Assignment.builder()
                .orgId(teacher.getOrgId())
                .teacherId(teacherId)
                .questId(request.questId())
                .title(request.title().trim())
                .dueAt(request.dueAt())
                .createdAt(now)
                .updatedAt(now)
                .build());
        return toResponse(saved, quest.getTitle(), List.of());
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listForTeacher(UUID teacherId) {
        Profile teacher = profileRepository
                .findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        assertPremiumLms(teacher);
        return assignmentRepository.findByOrgIdOrderByCreatedAtDesc(teacher.getOrgId()).stream()
                .map(this::toResponseWithSubmissions)
                .toList();
    }

    @Transactional
    public void autoGradeOnQuestCompletion(UUID studentId, UUID questId) {
        Profile student = profileRepository
                .findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        UUID studentOrgId = student.getOrgId();
        if (studentOrgId == null) {
            return;
        }
        boolean isStudentInOrg = organizationMemberRepository
                .findByOrganizationIdAndUserId(studentOrgId, studentId)
                .map(member -> "student".equalsIgnoreCase(member.getOrgRole()))
                .orElse(false);
        if (!isStudentInOrg) {
            return;
        }
        List<Assignment> assignments = assignmentRepository.findByOrgIdOrderByCreatedAtDesc(studentOrgId).stream()
                .filter(a -> a.getQuestId().equals(questId))
                .toList();
        if (assignments.isEmpty()) {
            return;
        }
        UserQuestProgress progress =
                userQuestProgressRepository.findByUserIdAndQuestId(studentId, questId).orElse(null);
        if (progress == null || !"completed".equalsIgnoreCase(progress.getStatus())) {
            return;
        }
        Instant now = Instant.now();
        for (Assignment assignment : assignments) {
            AssignmentSubmission submission = assignmentSubmissionRepository
                    .findByAssignmentIdAndStudentId(assignment.getId(), studentId)
                    .orElse(AssignmentSubmission.builder()
                            .assignmentId(assignment.getId())
                            .studentId(studentId)
                            .createdAt(now)
                            .build());
            submission.setScore(100);
            submission.setAutoGraded(true);
            submission.setCompletedAt(now);
            submission.setUpdatedAt(now);
            assignmentSubmissionRepository.save(submission);
        }
    }

    private AssignmentResponse toResponseWithSubmissions(Assignment assignment) {
        Quest quest = questRepository.findById(assignment.getQuestId()).orElse(null);
        String questTitle = quest != null ? quest.getTitle() : "Quest";
        List<AssignmentSubmission> submissions =
                assignmentSubmissionRepository.findByAssignmentId(assignment.getId());
        Map<UUID, Profile> profiles = new HashMap<>();
        profileRepository
                .findAllById(submissions.stream().map(AssignmentSubmission::getStudentId).toList())
                .forEach(p -> profiles.put(p.getId(), p));
        List<AssignmentSubmissionResponse> rows = submissions.stream()
                .map(s -> new AssignmentSubmissionResponse(
                        s.getId(),
                        s.getStudentId(),
                        profiles.containsKey(s.getStudentId())
                                ? profiles.get(s.getStudentId()).getDisplayName()
                                : "Student",
                        s.getScore(),
                        s.isAutoGraded(),
                        s.getCompletedAt()))
                .toList();
        return toResponse(assignment, questTitle, rows);
    }

    private AssignmentResponse toResponse(
            Assignment assignment, String questTitle, List<AssignmentSubmissionResponse> submissions) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getQuestId(),
                questTitle,
                assignment.getDueAt(),
                assignment.getCreatedAt(),
                submissions);
    }

    private void assertPremiumLms(Profile teacher) {
        if (teacher.getOrgId() == null) {
            throw new BusinessRuleException("LMS chỉ dành cho tổ chức B2B Premium");
        }
        OrganizationMember member = organizationMemberRepository
                .findByOrganizationIdAndUserId(teacher.getOrgId(), teacher.getId())
                .orElseThrow(() -> new BusinessRuleException("Bạn không thuộc tổ chức này"));
        if (!"teacher".equalsIgnoreCase(member.getOrgRole())) {
            throw new BusinessRuleException("Chỉ giáo viên mới có quyền dùng LMS");
        }
        Organization org = organizationRepository
                .findById(teacher.getOrgId())
                .orElseThrow(() -> new BusinessRuleException("Tổ chức không tồn tại"));
        String plan = org.getPlanType() != null ? org.getPlanType() : org.getPlan();
        if (plan == null || !"PREMIUM".equalsIgnoreCase(plan.trim())) {
            throw new LmsPremiumRequiredException("LMS đầy đủ chỉ có ở gói Premium B2B");
        }
    }
}

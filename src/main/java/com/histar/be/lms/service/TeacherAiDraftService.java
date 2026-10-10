package com.histar.be.lms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.chat.evaluation.HistoricalContentEvaluator;
import com.histar.be.chat.evaluation.HistoricalEvaluation;
import com.histar.be.chat.guided.GuidedAnswerBlock;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.lms.dto.AssignmentResponse;
import com.histar.be.lms.dto.CreateAssignmentRequest;
import com.histar.be.lms.dto.CreateTeacherAiDraftRequest;
import com.histar.be.lms.dto.TeacherAiDraftResponse;
import com.histar.be.lms.entity.TeacherAiDraft;
import com.histar.be.lms.entity.TeacherAiDraftStatus;
import com.histar.be.lms.repository.TeacherAiDraftRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.observability.ProductionMetrics;
import com.histar.be.quest.campaign.QuestCampaign;
import com.histar.be.quest.campaign.QuestCampaignRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherAiDraftService {
    private static final TypeReference<List<GuidedAnswerBlock>> BLOCKS = new TypeReference<>() {};
    private static final TypeReference<Set<String>> LOCALES = new TypeReference<>() {};
    private static final TypeReference<List<String>> REASONS = new TypeReference<>() {};

    private final TeacherAiDraftRepository draftRepository;
    private final LmsAssignmentService lmsAssignmentService;
    private final HistoricalContentEvaluator historicalContentEvaluator;
    private final ObjectMapper objectMapper;
    private final ProductionMetrics productionMetrics;
    private final QuestCampaignRepository campaignRepository;

    @Transactional
    public TeacherAiDraftResponse create(UUID teacherId, CreateTeacherAiDraftRequest request) {
        Profile teacher = lmsAssignmentService.requirePremiumTeacher(teacherId);
        Instant now = Instant.now();
        TeacherAiDraft draft = newDraft(teacher, request, now);
        return toResponse(draftRepository.save(draft));
    }

    @Transactional(readOnly = true)
    public List<TeacherAiDraftResponse> list(UUID teacherId) {
        lmsAssignmentService.requirePremiumTeacher(teacherId);
        return draftRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public TeacherAiDraftResponse update(UUID teacherId, UUID draftId, CreateTeacherAiDraftRequest request) {
        lmsAssignmentService.requirePremiumTeacher(teacherId);
        TeacherAiDraft draft = ownedDraft(teacherId, draftId);
        if (draft.getStatus() == TeacherAiDraftStatus.PUBLISHED) {
            throw new BusinessRuleException("Không thể sửa draft đã publish");
        }
        applyRequest(draft, request);
        draft.setStatus(TeacherAiDraftStatus.DRAFT);
        draft.setUpdatedAt(Instant.now());
        return toResponse(draftRepository.save(draft));
    }

    @Transactional
    public TeacherAiDraftResponse review(UUID teacherId, UUID draftId) {
        lmsAssignmentService.requirePremiumTeacher(teacherId);
        TeacherAiDraft draft = ownedDraft(teacherId, draftId);
        if (draft.getStatus() != TeacherAiDraftStatus.DRAFT) {
            throw new BusinessRuleException("Chỉ draft đang soạn mới có thể review");
        }
        if (!draft.isEvaluationPassed()) {
            throw new BusinessRuleException("Draft chưa đạt historical evaluation");
        }
        draft.setStatus(TeacherAiDraftStatus.REVIEWED);
        draft.setUpdatedAt(Instant.now());
        return toResponse(draftRepository.save(draft));
    }

    @Transactional
    public TeacherAiDraftResponse publish(UUID teacherId, UUID draftId) {
        lmsAssignmentService.requirePremiumTeacher(teacherId);
        TeacherAiDraft draft = ownedDraft(teacherId, draftId);
        if (draft.getStatus() != TeacherAiDraftStatus.REVIEWED) {
            throw new BusinessRuleException("Draft phải được review trước khi publish");
        }
        if (!draft.isEvaluationPassed()) {
            throw new BusinessRuleException("Draft chưa đạt historical evaluation");
        }
        AssignmentResponse assignment = lmsAssignmentService.createAssignment(
                teacherId, new CreateAssignmentRequest(draft.getTitle(), draft.getQuestId(), draft.getCampaignId(), draft.getDueAt()));
        draft.setPublishedAssignmentId(assignment.id());
        draft.setStatus(TeacherAiDraftStatus.PUBLISHED);
        draft.setUpdatedAt(Instant.now());
        return toResponse(draftRepository.save(draft));
    }

    private TeacherAiDraft newDraft(Profile teacher, CreateTeacherAiDraftRequest request, Instant now) {
        TeacherAiDraft draft = TeacherAiDraft.builder()
                .orgId(teacher.getOrgId())
                .teacherId(teacher.getId())
                .questId(request.questId())
                .campaignId(request.campaignId())
                .title(request.title().trim())
                .sourceVersion(request.sourceVersion().trim())
                .dueAt(request.dueAt())
                .status(TeacherAiDraftStatus.DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();
        applyRequest(draft, request);
        return draft;
    }

    private void applyRequest(TeacherAiDraft draft, CreateTeacherAiDraftRequest request) {
        HistoricalEvaluation evaluation = historicalContentEvaluator.evaluate(request.answerBlocks(), request.approvedLocales());
        productionMetrics.recordTeacherDraftEvaluation(evaluation.passed());
        draft.setQuestId(request.questId());
        QuestCampaign campaign = campaignRepository.findById(request.campaignId())
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
        if (!request.questId().equals(campaign.getQuestId())) {
            throw new BusinessRuleException("Campaign không thuộc quest đã chọn");
        }
        draft.setCampaignId(request.campaignId());
        draft.setTitle(request.title().trim());
        draft.setSourceVersion(request.sourceVersion().trim());
        draft.setAnswerBlocksJson(write(request.answerBlocks()));
        draft.setApprovedLocalesJson(write(request.approvedLocales()));
        draft.setEvaluationPassed(evaluation.passed());
        draft.setEvaluationReasons(write(evaluation.reasons()));
        draft.setDueAt(request.dueAt());
    }

    private TeacherAiDraft ownedDraft(UUID teacherId, UUID draftId) {
        TeacherAiDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher AI draft not found"));
        if (!teacherId.equals(draft.getTeacherId())) {
            throw new BusinessRuleException("Bạn không sở hữu draft này");
        }
        return draft;
    }

    private TeacherAiDraftResponse toResponse(TeacherAiDraft draft) {
        return new TeacherAiDraftResponse(
                draft.getId(), draft.getTitle(), draft.getQuestId(), draft.getCampaignId(), draft.getSourceVersion(),
                read(draft.getAnswerBlocksJson(), BLOCKS), read(draft.getApprovedLocalesJson(), LOCALES),
                draft.isEvaluationPassed(), read(draft.getEvaluationReasons(), REASONS), draft.getStatus(), draft.getDueAt(),
                draft.getPublishedAssignmentId(), draft.getCreatedAt(), draft.getUpdatedAt());
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("Không thể lưu nội dung draft");
        }
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("Dữ liệu draft không hợp lệ");
        }
    }
}

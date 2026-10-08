package com.histar.be.lms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.chat.evaluation.HistoricalContentEvaluator;
import com.histar.be.chat.dto.ChatSource;
import com.histar.be.chat.guided.GuidedAnswerBlock;
import com.histar.be.chat.guided.GuidedAnswerType;
import com.histar.be.chat.guided.GuidedConfidence;
import com.histar.be.lms.dto.CreateTeacherAiDraftRequest;
import com.histar.be.lms.entity.TeacherAiDraft;
import com.histar.be.lms.entity.TeacherAiDraftStatus;
import com.histar.be.lms.repository.TeacherAiDraftRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.observability.ProductionMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeacherAiDraftServiceTest {

    @Mock private TeacherAiDraftRepository draftRepository;
    @Mock private LmsAssignmentService lmsAssignmentService;
    private TeacherAiDraftService service;

    @BeforeEach
    void setUp() {
        service = new TeacherAiDraftService(
                draftRepository, lmsAssignmentService, new HistoricalContentEvaluator(), new ObjectMapper(), new ProductionMetrics(new SimpleMeterRegistry()));
    }

    @Test
    void creates_a_draft_with_a_persisted_evaluation_snapshot() {
        UUID teacherId = UUID.randomUUID();
        Profile teacher = Profile.builder().id(teacherId).orgId(UUID.randomUUID()).build();
        when(lmsAssignmentService.requirePremiumTeacher(teacherId)).thenReturn(teacher);
        when(draftRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(teacherId, validRequest());

        ArgumentCaptor<TeacherAiDraft> saved = ArgumentCaptor.forClass(TeacherAiDraft.class);
        verify(draftRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(TeacherAiDraftStatus.DRAFT);
        assertThat(saved.getValue().isEvaluationPassed()).isTrue();
        assertThat(saved.getValue().getEvaluationReasons()).isEqualTo("[]");
    }

    @Test
    void refuses_to_publish_a_draft_that_has_not_been_reviewed() {
        UUID teacherId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        Profile teacher = Profile.builder().id(teacherId).orgId(UUID.randomUUID()).build();
        when(lmsAssignmentService.requirePremiumTeacher(teacherId)).thenReturn(teacher);
        when(draftRepository.findById(draftId)).thenReturn(Optional.of(TeacherAiDraft.builder()
                .id(draftId)
                .teacherId(teacherId)
                .status(TeacherAiDraftStatus.DRAFT)
                .build()));

        assertThatThrownBy(() -> service.publish(teacherId, draftId))
                .hasMessageContaining("review");
    }

    private CreateTeacherAiDraftRequest validRequest() {
        return new CreateTeacherAiDraftRequest(
                "Củ Chi — kiểm tra chứng cứ",
                UUID.randomUUID(),
                "cu-chi-v1",
                List.of(new GuidedAnswerBlock(
                        GuidedAnswerType.VERIFIED_FACT,
                        GuidedConfidence.VERIFIED,
                        "Bếp Hoàng Cầm dẫn khói ra xa.",
                        List.of(new ChatSource("Bảo tàng Củ Chi · 2025", "Bếp Hoàng Cầm", null)))),
                Set.of("vi", "en", "ko", "zh-CN"),
                null);
    }
}

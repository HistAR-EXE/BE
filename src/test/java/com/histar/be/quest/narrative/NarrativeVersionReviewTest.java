package com.histar.be.quest.narrative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.admin.audit.AdminAuditEvent;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.common.exception.BaseException;
import com.histar.be.quest.narrative.entity.*;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NarrativeVersionReviewTest {
    @Mock NarrativeInvestigationRepository investigations;
    @Mock NarrativeInvestigationVersionRepository versions;
    @Mock NarrativeInvestigationReviewRepository reviews;
    @Mock AdminAuditService audit;
    NarrativeContentReviewService service;
    NarrativeInvestigation parent;
    NarrativeInvestigationVersion draft;

    @BeforeEach
    void setUp() {
        service = new NarrativeContentReviewService(investigations, versions, reviews, audit,
                new ObjectMapper(), new NarrativeQuestGraphValidator());
        parent = NarrativeInvestigation.builder().id(UUID.randomUUID()).questId(UUID.randomUUID()).build();
        draft = NarrativeInvestigationVersion.builder().id(UUID.randomUUID()).investigationId(parent.getId())
                .version(2).revision(3).openDraft(true).reviewStatus("DRAFT").approvedLocalesJson("[]")
                .graphJson("{\"startNodeCode\":\"start\",\"sources\":[{\"sourceId\":\"s\",\"version\":\"1\",\"locale\":\"vi\",\"citation\":\"citation\"}],\"nodes\":[{\"code\":\"start\",\"type\":\"OBSERVATION\",\"choices\":[]}],\"evidenceNodeCodes\":[],\"outcomeNodeCodes\":[]}")
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(investigations.findForUpdateByQuestId(parent.getQuestId())).thenReturn(Optional.of(parent));
        when(versions.findByInvestigationIdAndVersion(parent.getId(), 2)).thenReturn(Optional.of(draft));
    }

    @Test
    void reviewRequiresCurrentRevisionAndLinksItsSingleAuditEvent() {
        UUID auditId = UUID.randomUUID();
        when(audit.append(any())).thenReturn(AdminAuditEvent.builder().id(auditId).build());
        when(reviews.save(any())).thenAnswer(call -> call.getArgument(0));
        when(versions.save(any())).thenAnswer(call -> call.getArgument(0));

        NarrativeReviewResponse result = service.review(parent.getQuestId(), UUID.randomUUID(), "reviewer",
                new NarrativeReviewRequest(NarrativeReviewDecision.APPROVED, Set.of("vi"),
                        "Historian approved", 2, 3));

        assertThat(result.version()).isEqualTo(2);
        assertThat(result.revision()).isEqualTo(3);
        assertThat(result.auditEventId()).isEqualTo(auditId);
        assertThat(draft.getReviewStatus()).isEqualTo("APPROVED");
    }

    @Test
    void reviewRejectsStaleRevision() {
        assertThatThrownBy(() -> service.review(parent.getQuestId(), UUID.randomUUID(), "reviewer",
                new NarrativeReviewRequest(NarrativeReviewDecision.APPROVED, Set.of("vi"),
                        "Historian approved", 2, 2)))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode().getCode())
                .isEqualTo("VERSION_CONFLICT");
    }
}

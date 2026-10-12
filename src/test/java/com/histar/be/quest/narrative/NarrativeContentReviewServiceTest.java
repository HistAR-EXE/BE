package com.histar.be.quest.narrative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.quest.narrative.entity.NarrativeInvestigation;
import com.histar.be.quest.narrative.entity.NarrativeInvestigationReview;
import com.histar.be.quest.narrative.entity.NarrativeReviewDecision;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NarrativeContentReviewServiceTest {

    private static final UUID QUEST_ID = UUID.fromString("33333333-3333-3333-3333-333333333338");

    @Mock private NarrativeInvestigationRepository investigations;
    @Mock private NarrativeInvestigationReviewRepository reviews;

    @Test
    void approvesAVietnameseOnlyGraphWhenEverySourceHasVietnameseCitation() {
        NarrativeInvestigation investigation = investigation(validVietnameseGraph());
        when(investigations.findByQuestId(QUEST_ID)).thenReturn(Optional.of(investigation));
        when(reviews.save(any(NarrativeInvestigationReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NarrativeContentReviewService service = service();

        var response = service.review(QUEST_ID, "reviewer@histar.vn",
                new NarrativeReviewRequest(NarrativeReviewDecision.APPROVED, Set.of("vi"), "Reviewed"));

        assertThat(response.approvedLocales()).containsExactly("vi");
        assertThat(investigation.getReviewStatus()).isEqualTo("APPROVED");
        assertThat(investigation.isEnabled()).isFalse();
    }

    @Test
    void rejectsApprovalWhenVietnameseCitationMetadataIsMissing() {
        NarrativeInvestigation investigation = investigation("""
                {"startNodeCode":"tour-360","sources":[{"sourceId":"dsvh-cu-chi","version":"2026-10-12","locale":"vi"}],
                "nodes":[{"code":"tour-360","type":"TOUR_360_OBSERVATION","choices":[]}],
                "evidenceNodeCodes":[],"outcomeNodeCodes":[]}
                """);
        when(investigations.findByQuestId(QUEST_ID)).thenReturn(Optional.of(investigation));

        assertThatThrownBy(() -> service().review(QUEST_ID, "reviewer@histar.vn",
                new NarrativeReviewRequest(NarrativeReviewDecision.APPROVED, Set.of("vi"), "Reviewed")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("source");
    }

    private NarrativeContentReviewService service() {
        return new NarrativeContentReviewService(investigations, reviews, new ObjectMapper(), new NarrativeQuestGraphValidator());
    }

    private NarrativeInvestigation investigation(String graphJson) {
        return NarrativeInvestigation.builder().id(UUID.randomUUID()).questId(QUEST_ID).version(2).enabled(false)
                .graphJson(graphJson).reviewStatus("DRAFT").approvedLocalesJson("[]")
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }

    private String validVietnameseGraph() {
        return """
                {"startNodeCode":"tour-360","sources":[{"sourceId":"dsvh-cu-chi","version":"2026-10-12","locale":"vi","citation":"Cổng Di sản văn hóa: Địa đạo Củ Chi","url":"https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490"}],
                "nodes":[{"code":"tour-360","type":"TOUR_360_OBSERVATION","choices":[]}],
                "evidenceNodeCodes":[],"outcomeNodeCodes":[]}
                """;
    }
}

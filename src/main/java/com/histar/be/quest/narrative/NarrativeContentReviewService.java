package com.histar.be.quest.narrative;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.quest.narrative.entity.*;
import com.histar.be.quest.narrative.model.NarrativeQuestGraph;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Specialist approval is explicit and append-only; it never silently enables a pilot. */
@Service @RequiredArgsConstructor
public class NarrativeContentReviewService {
    private static final Set<String> REQUIRED_LOCALES = Set.of("vi", "en", "ko", "zh-CN");
    private final NarrativeInvestigationRepository investigations;
    private final NarrativeInvestigationReviewRepository reviews;
    private final ObjectMapper objectMapper;
    private final NarrativeQuestGraphValidator graphValidator;

    @Transactional
    public NarrativeReviewResponse review(UUID questId, String reviewer, NarrativeReviewRequest request) {
        NarrativeInvestigation investigation = investigations.findByQuestId(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Narrative investigation not found"));
        Set<String> locales = request.approvedLocales() == null ? Set.of() : Set.copyOf(request.approvedLocales());
        if (request.decision() == NarrativeReviewDecision.APPROVED && !locales.containsAll(REQUIRED_LOCALES)) {
            throw new BusinessRuleException("Chỉ được duyệt khi đủ bản nội dung đã duyệt: vi, en, ko, zh-CN");
        }
        if (request.decision() == NarrativeReviewDecision.APPROVED) {
            validateApprovedGraph(investigation.getGraphJson(), locales);
        }
        Instant now = Instant.now();
        investigation.setReviewStatus(request.decision().name());
        investigation.setApprovedLocalesJson(write(locales));
        investigation.setReviewedBy(reviewer);
        investigation.setReviewedAt(now);
        if (request.decision() == NarrativeReviewDecision.REJECTED) investigation.setEnabled(false);
        investigations.save(investigation);
        NarrativeInvestigationReview entry = reviews.save(NarrativeInvestigationReview.builder()
                .investigationId(investigation.getId()).decision(request.decision()).approvedLocalesJson(write(locales))
                .reviewNote(trim(request.note())).reviewedBy(reviewer).reviewedAt(now).build());
        return response(entry, locales);
    }

    @Transactional(readOnly = true)
    public List<NarrativeReviewResponse> history(UUID questId) {
        NarrativeInvestigation investigation = investigations.findByQuestId(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Narrative investigation not found"));
        return reviews.findByInvestigationIdOrderByReviewedAtDesc(investigation.getId()).stream()
                .map(entry -> response(entry, read(entry.getApprovedLocalesJson()))).toList();
    }

    private void validateApprovedGraph(String graphJson, Set<String> approvedLocales) {
        try {
            Map<String, Object> graph = objectMapper.readValue(graphJson, new TypeReference<>() {});
            NarrativeQuestGraph typedGraph = objectMapper.convertValue(graph, NarrativeQuestGraph.class);
            List<String> structuralErrors = graphValidator.validate(typedGraph);
            if (!structuralErrors.isEmpty()) {
                throw new BusinessRuleException("Không thể duyệt graph không hợp lệ: " + String.join("; ", structuralErrors));
            }
            Object sources = graph.get("sources");
            if (!(sources instanceof Collection<?> sourceList) || sourceList.isEmpty()) {
                throw new BusinessRuleException("Không thể duyệt graph chưa có source/version/locale/citation");
            }
            boolean wellFormed = sourceList.stream().allMatch(source -> source instanceof Map<?, ?> map
                    && nonBlank(map.get("sourceId")) && nonBlank(map.get("version"))
                    && nonBlank(map.get("locale")) && nonBlank(map.get("citation")));
            if (!wellFormed) {
                throw new BusinessRuleException("Không thể duyệt source thiếu sourceId/version/locale/citation");
            }
            Set<String> sourceLocales = sourceList.stream().map(source -> ((Map<?, ?>) source).get("locale"))
                    .filter(String.class::isInstance).map(String.class::cast).collect(java.util.stream.Collectors.toSet());
            if (!sourceLocales.containsAll(approvedLocales)) {
                throw new BusinessRuleException("Mỗi locale được duyệt phải có citation source/version tương ứng");
            }
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("Không thể duyệt graph không đọc được");
        }
    }
    private boolean nonBlank(Object value) { return value instanceof String text && !text.isBlank(); }
    private String write(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException e) { throw new BusinessRuleException("Không thể lưu review"); } }
    private Set<String> read(String json) { try { return objectMapper.readValue(json, new TypeReference<Set<String>>() {}); } catch (JsonProcessingException e) { throw new BusinessRuleException("Review data không đọc được"); } }
    private NarrativeReviewResponse response(NarrativeInvestigationReview entry, Set<String> locales) { return new NarrativeReviewResponse(entry.getInvestigationId(), entry.getDecision(), locales, entry.getReviewedBy(), entry.getReviewedAt(), entry.getReviewNote()); }
    private String trim(String note) { return note == null ? null : note.trim(); }
}

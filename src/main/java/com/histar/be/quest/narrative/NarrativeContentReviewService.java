package com.histar.be.quest.narrative;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditEvent;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.common.exception.*;
import com.histar.be.quest.narrative.entity.*;
import com.histar.be.quest.narrative.model.NarrativeQuestGraph;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Specialist review is version-scoped and append-only; it never publishes content. */
@Service
public class NarrativeContentReviewService {
    private static final Set<String> REQUIRED_LOCALES = Set.of("vi");
    private final NarrativeInvestigationRepository investigations;
    private final NarrativeInvestigationVersionRepository versions;
    private final NarrativeInvestigationReviewRepository reviews;
    private final AdminAuditService audit;
    private final ObjectMapper objectMapper;
    private final NarrativeQuestGraphValidator graphValidator;

    @Autowired
    public NarrativeContentReviewService(NarrativeInvestigationRepository investigations,
            NarrativeInvestigationVersionRepository versions, NarrativeInvestigationReviewRepository reviews,
            AdminAuditService audit, ObjectMapper objectMapper, NarrativeQuestGraphValidator graphValidator) {
        this.investigations = investigations;
        this.versions = versions;
        this.reviews = reviews;
        this.audit = audit;
        this.objectMapper = objectMapper;
        this.graphValidator = graphValidator;
    }

    /** Compatibility constructor retained for focused Phase 1 tests only. */
    public NarrativeContentReviewService(NarrativeInvestigationRepository investigations,
            NarrativeInvestigationReviewRepository reviews, ObjectMapper objectMapper,
            NarrativeQuestGraphValidator graphValidator) {
        this(investigations, null, reviews, null, objectMapper, graphValidator);
    }

    @Transactional
    public NarrativeReviewResponse review(UUID questId, UUID actorId, String reviewer, NarrativeReviewRequest request) {
        if (versions == null) return legacyReview(questId, reviewer, request);
        if (request.version() <= 0) {
            throw new BusinessRuleException("Narrative draft version is required");
        }
        NarrativeInvestigation parent = investigations.findForUpdateByQuestId(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Narrative investigation not found"));
        NarrativeInvestigationVersion draft = versions.findByInvestigationIdAndVersion(parent.getId(), request.version())
                .orElseThrow(() -> new ResourceNotFoundException("Narrative version not found"));
        if (!draft.isOpenDraft()) throw new BusinessRuleException("Only the open draft can be reviewed");
        if (draft.getRevision() != request.revision()) {
            throw new NarrativeConflictException(ErrorCode.VERSION_CONFLICT, "Narrative draft revision is stale");
        }
        String note = requireNote(request.note());
        Set<String> locales = Set.copyOf(request.approvedLocales());
        validateDecision(request.decision(), locales, draft.getGraphJson());
        Instant now = Instant.now();
        draft.setReviewStatus(request.decision().name());
        draft.setApprovedLocalesJson(write(locales));
        draft.setReviewedBy(reviewer);
        draft.setReviewedAt(now);
        draft.setUpdatedAt(now);
        versions.save(draft);
        AdminAuditEvent event = audit.append(new AdminAuditCommand(actorId, "NARRATIVE_INVESTIGATION",
                parent.getId(), "REVIEW_" + request.decision().name(), draft.getRevision(), draft.getRevision(),
                note, null, Map.of("questId", questId.toString(), "version", draft.getVersion(),
                        "revision", draft.getRevision())));
        NarrativeInvestigationReview entry = reviews.save(NarrativeInvestigationReview.builder()
                .investigationId(parent.getId()).versionId(draft.getId()).version(draft.getVersion())
                .revision(draft.getRevision()).auditEventId(event.getId()).decision(request.decision())
                .approvedLocalesJson(write(locales)).reviewNote(note).reviewedBy(reviewer).reviewedAt(now).build());
        return response(entry, locales);
    }

    @Transactional
    public NarrativeReviewResponse review(UUID questId, String reviewer, NarrativeReviewRequest request) {
        return review(questId, null, reviewer, request);
    }

    @Transactional(readOnly = true)
    public List<NarrativeReviewResponse> history(UUID questId) {
        NarrativeInvestigation parent = investigations.findByQuestId(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Narrative investigation not found"));
        return reviews.findByInvestigationIdOrderByReviewedAtDesc(parent.getId()).stream()
                .map(entry -> response(entry, read(entry.getApprovedLocalesJson()))).toList();
    }

    private NarrativeReviewResponse legacyReview(UUID questId, String reviewer, NarrativeReviewRequest request) {
        NarrativeInvestigation investigation = investigations.findByQuestId(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Narrative investigation not found"));
        Set<String> locales = Set.copyOf(request.approvedLocales());
        validateDecision(request.decision(), locales, investigation.getGraphJson());
        Instant now = Instant.now();
        investigation.setReviewStatus(request.decision().name());
        investigation.setApprovedLocalesJson(write(locales));
        investigation.setReviewedBy(reviewer);
        investigation.setReviewedAt(now);
        investigations.save(investigation);
        NarrativeInvestigationReview entry = reviews.save(NarrativeInvestigationReview.builder()
                .investigationId(investigation.getId()).version(investigation.getVersion()).revision(0L)
                .decision(request.decision()).approvedLocalesJson(write(locales)).reviewNote(trim(request.note()))
                .reviewedBy(reviewer).reviewedAt(now).build());
        return response(entry, locales);
    }

    private void validateDecision(NarrativeReviewDecision decision, Set<String> locales, String graphJson) {
        if (decision == NarrativeReviewDecision.APPROVED && !locales.containsAll(REQUIRED_LOCALES)) {
            throw new BusinessRuleException("Approval requires reviewed locale vi");
        }
        if (decision == NarrativeReviewDecision.APPROVED) validateApprovedGraph(graphJson, locales);
    }

    private void validateApprovedGraph(String graphJson, Set<String> approvedLocales) {
        try {
            Map<String, Object> graph = objectMapper.readValue(graphJson, new TypeReference<>() {});
            List<String> errors = graphValidator.validate(objectMapper.convertValue(graph, NarrativeQuestGraph.class));
            if (!errors.isEmpty()) throw new BusinessRuleException("Cannot approve invalid graph: " + String.join("; ", errors));
            Object sources = graph.get("sources");
            if (!(sources instanceof Collection<?> sourceList) || sourceList.isEmpty()) {
                throw new BusinessRuleException("Cannot approve graph without source/version/locale/citation");
            }
            boolean valid = sourceList.stream().allMatch(source -> source instanceof Map<?, ?> map
                    && nonBlank(map.get("sourceId")) && nonBlank(map.get("version"))
                    && nonBlank(map.get("locale")) && nonBlank(map.get("citation")));
            if (!valid) throw new BusinessRuleException("Cannot approve malformed source metadata");
            Set<String> sourceLocales = new HashSet<>();
            sourceList.forEach(source -> { Object locale = ((Map<?, ?>) source).get("locale"); if (locale instanceof String text) sourceLocales.add(text); });
            if (!sourceLocales.containsAll(approvedLocales)) throw new BusinessRuleException("Every approved locale requires a citation");
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("Cannot approve unreadable graph");
        }
    }

    private String requireNote(String note) {
        String value = trim(note);
        if (value == null || value.length() < 10) throw new BusinessRuleException("Review note must be at least 10 characters");
        return value;
    }
    private boolean nonBlank(Object value) { return value instanceof String text && !text.isBlank(); }
    private String write(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException e) { throw new BusinessRuleException("Cannot store review"); } }
    private Set<String> read(String json) { try { return objectMapper.readValue(json, new TypeReference<Set<String>>() {}); } catch (JsonProcessingException e) { return Set.of(); } }
    private String trim(String note) { return note == null ? null : note.trim(); }
    private NarrativeReviewResponse response(NarrativeInvestigationReview entry, Set<String> locales) {
        return new NarrativeReviewResponse(entry.getInvestigationId(), entry.getVersion() == null ? 0 : entry.getVersion(),
                entry.getRevision() == null ? 0 : entry.getRevision(), entry.getDecision(), locales,
                entry.getReviewedBy(), entry.getReviewedAt(), entry.getReviewNote(), entry.getAuditEventId());
    }
}

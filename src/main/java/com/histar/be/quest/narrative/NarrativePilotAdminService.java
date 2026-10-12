package com.histar.be.quest.narrative;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.config.PilotFeatureProperties;
import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.narrative.entity.NarrativeInvestigation;
import com.histar.be.quest.narrative.model.NarrativeQuestGraph;
import com.histar.be.quest.repository.QuestRepository;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NarrativePilotAdminService {
    private static final Set<String> REQUIRED_LOCALES = Set.of("vi");

    private final NarrativeInvestigationRepository investigations;
    private final QuestRepository quests;
    private final ObjectMapper objectMapper;
    private final NarrativeQuestGraphValidator graphValidator;
    private final PilotFeatureProperties pilotFeatures;
    private final AdminAuditService audit;

    @Transactional(readOnly = true)
    public List<NarrativePilotAdminResponse> list() {
        return investigations.findAll().stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public NarrativePilotAdminResponse readiness(UUID questId) {
        return response(find(questId));
    }

    @Transactional
    public NarrativePilotAdminResponse enable(UUID questId, UUID actorId, String reason) {
        NarrativeInvestigation investigation = find(questId);
        Readiness readiness = readinessOf(investigation);
        if (!readiness.errors().isEmpty()) {
            throw new BusinessRuleException("Pilot chưa đủ điều kiện: " + String.join("; ", readiness.errors()));
        }
        investigation.setEnabled(true);
        investigation.setUpdatedAt(Instant.now());
        NarrativeInvestigation saved = investigations.save(investigation);
        audit.append(new AdminAuditCommand(actorId, "NARRATIVE_INVESTIGATION", saved.getId(), "ENABLED",
                null, null, reason.trim(), null, Map.of("questId", questId.toString(), "version", saved.getVersion())));
        return response(saved);
    }

    @Transactional
    public NarrativePilotAdminResponse disable(UUID questId, UUID actorId, String reason) {
        NarrativeInvestigation investigation = find(questId);
        investigation.setEnabled(false);
        investigation.setUpdatedAt(Instant.now());
        NarrativeInvestigation saved = investigations.save(investigation);
        audit.append(new AdminAuditCommand(actorId, "NARRATIVE_INVESTIGATION", saved.getId(), "DISABLED",
                null, null, reason.trim(), null, Map.of("questId", questId.toString(), "version", saved.getVersion())));
        return response(saved);
    }

    @Transactional(readOnly = true)
    public NarrativeDraftResponse draft(UUID questId) {
        NarrativeInvestigation investigation = find(questId);
        return new NarrativeDraftResponse(questId, investigation.getVersion(), investigation.getGraphJson());
    }

    @Transactional
    public NarrativeDraftResponse updateDraft(UUID questId, UUID actorId, NarrativeDraftUpdateRequest request) {
        NarrativeInvestigation investigation = find(questId);
        if (investigation.getVersion() != request.expectedVersion()) {
            throw new BusinessRuleException("Graph đã được thay đổi; hãy tải lại bản nháp trước khi lưu");
        }
        validateDraftGraph(request.graphJson());
        int nextVersion = investigation.getVersion() + 1;
        investigation.setVersion(nextVersion);
        investigation.setGraphJson(request.graphJson().trim());
        investigation.setEnabled(false);
        investigation.setReviewStatus("DRAFT");
        investigation.setApprovedLocalesJson("[]");
        investigation.setReviewedBy(null);
        investigation.setReviewedAt(null);
        investigation.setUpdatedAt(Instant.now());
        NarrativeInvestigation saved = investigations.save(investigation);
        audit.append(new AdminAuditCommand(actorId, "NARRATIVE_INVESTIGATION", saved.getId(), "DRAFT_UPDATED",
                (long) request.expectedVersion(), (long) nextVersion, request.reason().trim(), null,
                Map.of("questId", questId.toString(), "version", nextVersion)));
        return new NarrativeDraftResponse(questId, nextVersion, saved.getGraphJson());
    }

    private NarrativeInvestigation find(UUID questId) {
        return investigations.findByQuestId(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Narrative investigation not found"));
    }

    private NarrativePilotAdminResponse response(NarrativeInvestigation investigation) {
        Quest quest = quests.findById(investigation.getQuestId())
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found: " + investigation.getQuestId()));
        Readiness readiness = readinessOf(investigation);
        return new NarrativePilotAdminResponse(
                investigation.getQuestId(), quest.getTitle(), investigation.getId(), investigation.getVersion(),
                investigation.isEnabled(), investigation.getReviewStatus(), readLocales(investigation.getApprovedLocalesJson()),
                investigation.getReviewedBy(), investigation.getReviewedAt(), pilotFeatures.isNarrativeInvestigation(),
                readiness.graphValid(), readiness.sourceCount(), readiness.errors());
    }

    private Readiness readinessOf(NarrativeInvestigation investigation) {
        List<String> errors = new ArrayList<>();
        if (!pilotFeatures.isNarrativeInvestigation()) errors.add("Feature flag PILOT_NARRATIVE_INVESTIGATION đang tắt");
        if (!"APPROVED".equals(investigation.getReviewStatus())) errors.add("Nội dung chưa được duyệt");
        Set<String> locales = readLocales(investigation.getApprovedLocalesJson());
        if (!locales.containsAll(REQUIRED_LOCALES)) errors.add("Chưa có locale vi được duyệt");
        int sourceCount = 0;
        boolean graphValid = false;
        try {
            JsonNode root = objectMapper.readTree(investigation.getGraphJson());
            JsonNode sources = root.get("sources");
            sourceCount = sources != null && sources.isArray() ? sources.size() : 0;
            if (sourceCount == 0) errors.add("Graph chưa có source");
            if (sources != null && sources.isArray()) {
                for (JsonNode source : sources) {
                    if (blank(source, "sourceId") || blank(source, "version") || blank(source, "locale") || blank(source, "citation")) {
                        errors.add("Source thiếu sourceId/version/locale/citation");
                        break;
                    }
                }
            }
            NarrativeQuestGraph graph = objectMapper.treeToValue(root, NarrativeQuestGraph.class);
            List<String> structural = graphValidator.validate(graph);
            graphValid = structural.isEmpty();
            errors.addAll(structural);
            if (sources == null || !sources.isArray() || !sourceLocales(sources).containsAll(locales)) {
                errors.add("Locale được duyệt chưa có citation tương ứng");
            }
        } catch (JsonProcessingException ex) {
            errors.add("Graph không đọc được");
        }
        return new Readiness(graphValid, sourceCount, errors.stream().distinct().toList());
    }

    private void validateDraftGraph(String graphJson) {
        try {
            JsonNode root = objectMapper.readTree(graphJson);
            NarrativeQuestGraph graph = objectMapper.treeToValue(root, NarrativeQuestGraph.class);
            List<String> structural = graphValidator.validate(graph);
            if (!structural.isEmpty()) throw new BusinessRuleException("Graph không hợp lệ: " + String.join("; ", structural));
            JsonNode sources = root.get("sources");
            if (sources == null || !sources.isArray() || sources.isEmpty()) throw new BusinessRuleException("Graph phải có ít nhất một source");
            for (JsonNode source : sources) {
                if (blank(source, "sourceId") || blank(source, "version") || blank(source, "locale") || blank(source, "citation")) {
                    throw new BusinessRuleException("Source thiếu sourceId/version/locale/citation");
                }
            }
        } catch (JsonProcessingException ex) {
            throw new BusinessRuleException("Graph JSON không đọc được");
        }
    }

    private boolean blank(JsonNode node, String field) { return !node.hasNonNull(field) || node.get(field).asText().isBlank(); }
    private Set<String> sourceLocales(JsonNode sources) {
        Set<String> locales = new HashSet<>();
        sources.forEach(source -> { if (source.hasNonNull("locale")) locales.add(source.get("locale").asText()); });
        return locales;
    }
    private Set<String> readLocales(String json) {
        try { return json == null ? Set.of() : objectMapper.readValue(json, new TypeReference<Set<String>>() {}); }
        catch (JsonProcessingException ex) { return Set.of(); }
    }
    private record Readiness(boolean graphValid, int sourceCount, List<String> errors) {}
}

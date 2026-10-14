package com.histar.be.quest.narrative;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.common.exception.*;
import com.histar.be.common.response.PageResponse;
import com.histar.be.config.PilotFeatureProperties;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.narrative.entity.*;
import com.histar.be.quest.narrative.model.NarrativeQuestGraph;
import com.histar.be.quest.repository.QuestRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NarrativePilotAdminService {
    private final NarrativeInvestigationRepository investigations;
    private final NarrativeInvestigationVersionRepository versions;
    private final NarrativeRolloutIdempotencyRepository idempotency;
    private final QuestRepository quests;
    private final ObjectMapper objectMapper;
    private final NarrativeQuestGraphValidator graphValidator;
    private final PilotFeatureProperties pilotFeatures;
    private final AdminAuditService audit;

    @Transactional(readOnly = true)
    public List<NarrativePilotAdminResponse> list() {
        return investigations.findAll().stream().map(this::listItem).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<NarrativePilotListResponse> list(int page, int size, String query,
            String reviewStatus, String rolloutState, String sort) {
        List<NarrativePilotListResponse> filtered = investigations.findAll().stream()
                .map(this::pageItem)
                .filter(item -> query == null || query.isBlank()
                        || item.questTitle().toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT)))
                .filter(item -> reviewStatus == null || reviewStatus.isBlank()
                        || reviewStatus.equalsIgnoreCase(item.reviewStatus()))
                .filter(item -> rolloutState == null || rolloutState.isBlank()
                        || rolloutState.equalsIgnoreCase(item.rolloutState().name()))
                .sorted(Comparator.comparing(NarrativePilotListResponse::questTitle,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (sort != null && sort.toLowerCase(Locale.ROOT).endsWith(",desc")) {
            filtered = new ArrayList<>(filtered);
            Collections.reverse(filtered);
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int from = Math.min(safePage * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        return PageResponse.<NarrativePilotListResponse>builder()
                .items(filtered.subList(from, to)).page(safePage).size(safeSize)
                .totalItems(filtered.size()).totalPages((int) Math.ceil(filtered.size() / (double) safeSize)).build();
    }

    @Transactional(readOnly = true)
    public NarrativeAdminDetailResponse detail(UUID questId) { return detailOf(find(questId)); }

    @Transactional
    public NarrativeVersionResponse createDraft(UUID questId, UUID actorId, NarrativeCreateDraftRequest request) {
        NarrativeInvestigation parent = lock(questId);
        if (versions.findByInvestigationIdAndOpenDraftTrue(parent.getId()).isPresent()) {
            throw conflict(ErrorCode.DRAFT_ALREADY_EXISTS, "An open narrative draft already exists");
        }
        requireParentVersion(parent, request.rowVersion());
        String graph = graphJson(request.graph(), request.graphJson());
        if (graph == null || graph.isBlank()) {
            graph = parent.getPublishedVersionId() == null ? parent.getGraphJson()
                    : versions.findById(parent.getPublishedVersionId()).orElseThrow().getGraphJson();
        }
        validateGraph(graph);
        int next = versions.findByInvestigationIdOrderByVersionDesc(parent.getId()).stream()
                .mapToInt(NarrativeInvestigationVersion::getVersion).max().orElse(0) + 1;
        Instant now = Instant.now();
        NarrativeInvestigationVersion saved = versions.save(NarrativeInvestigationVersion.builder()
                .investigationId(parent.getId()).version(next).revision(0).openDraft(true)
                .graphJson(graph.trim()).reviewStatus("DRAFT").approvedLocalesJson("[]")
                .createdAt(now).updatedAt(now).build());
        parent.setVersion(Math.max(parent.getVersion(), next));
        bump(parent);
        audit(actorId, parent, "DRAFT_CREATED", reason(request.reason(), "Tạo draft từ Admin Console"), null, 0L, next, null);
        return response(saved);
    }

    @Transactional
    public NarrativeVersionResponse patchDraft(UUID questId, int version, UUID actorId, NarrativePatchDraftRequest request) {
        NarrativeInvestigation parent = lock(questId);
        NarrativeInvestigationVersion draft = version(parent, version);
        if (!draft.isOpenDraft()) throw new BusinessRuleException("Published narrative versions are immutable");
        if (draft.getRevision() != request.rowVersion()) {
            throw conflict(ErrorCode.VERSION_CONFLICT, "Narrative draft revision is stale");
        }
        String graph = graphJson(request.graph(), request.graphJson());
        validateGraph(graph);
        long old = draft.getRevision();
        draft.setGraphJson(graph.trim());
        draft.setRevision(old + 1);
        draft.setReviewStatus("DRAFT");
        draft.setApprovedLocalesJson("[]");
        draft.setReviewedBy(null);
        draft.setReviewedAt(null);
        draft.setUpdatedAt(Instant.now());
        NarrativeInvestigationVersion saved = versions.save(draft);
        audit(actorId, parent, "DRAFT_UPDATED", reason(request.reason(), "Cập nhật draft từ Admin Console"), old, saved.getRevision(), version, null);
        return response(saved);
    }

    @Transactional(readOnly = true)
    public NarrativeReadinessResponse readiness(UUID questId, int draftVersion) {
        NarrativeInvestigation parent = find(questId);
        List<NarrativeReadinessItem> checks = checklist(version(parent, draftVersion));
        return new NarrativeReadinessResponse(questId, draftVersion,
                checks.stream().allMatch(NarrativeReadinessItem::passed),
                pilotFeatures.isNarrativeInvestigation(), checks);
    }

    @Transactional
    public NarrativeAdminDetailResponse enable(UUID questId, int version, UUID actorId, String reason, String key) {
        NarrativeInvestigation parent = lock(questId);
        return enableLocked(parent, version, null, actorId, reason, key);
    }

    private NarrativeAdminDetailResponse enableLocked(NarrativeInvestigation parent, int version, Long expectedRowVersion,
            UUID actorId, String reason, String key) {
        return idempotent(parent, "ENABLE", version, expectedRowVersion, reason, key, () -> {
            requireParentVersion(parent, expectedRowVersion);
            if (parent.getPublishedVersionId() != null || parent.getRolloutState() != NarrativeRolloutState.NOT_RELEASED) {
                throw new BusinessRuleException("Enable is only valid for the first publish");
            }
            long oldRowVersion = parent.getRowVersion();
            NarrativeRolloutState oldState = parent.getRolloutState();
            NarrativeInvestigationVersion candidate = candidate(parent, version);
            close(candidate);
            publish(parent, candidate, NarrativeRolloutState.ACTIVE);
            auditRollout(actorId, parent, "ENABLED", reason, oldRowVersion, parent.getRowVersion(), version,
                    key, oldState, parent.getRolloutState());
            return detailOf(parent);
        });
    }

    @Transactional
    public NarrativeAdminDetailResponse enable(UUID questId, int version, long rowVersion, UUID actorId, String reason, String key) {
        NarrativeInvestigation parent = lock(questId);
        return enableLocked(parent, version, rowVersion, actorId, reason, key);
    }

    @Transactional
    public NarrativeAdminDetailResponse pause(UUID questId, UUID actorId, String reason, String key) {
        NarrativeInvestigation parent = lock(questId);
        return pauseLocked(parent, null, actorId, reason, key);
    }

    private NarrativeAdminDetailResponse pauseLocked(NarrativeInvestigation parent, Long expectedRowVersion,
            UUID actorId, String reason, String key) {
        return idempotent(parent, "PAUSE", null, expectedRowVersion, reason, key, () -> {
            requireParentVersion(parent, expectedRowVersion);
            published(parent);
            if (parent.getRolloutState() != NarrativeRolloutState.ACTIVE) {
                throw new BusinessRuleException("Only an active narrative pilot can be paused");
            }
            long oldRowVersion = parent.getRowVersion();
            NarrativeRolloutState oldState = parent.getRolloutState();
            parent.setRolloutState(NarrativeRolloutState.PAUSED);
            parent.setEnabled(false);
            bump(parent);
            auditRollout(actorId, parent, "PAUSED", reason, oldRowVersion, parent.getRowVersion(), null,
                    key, oldState, parent.getRolloutState());
            return detailOf(parent);
        });
    }

    @Transactional
    public NarrativeAdminDetailResponse pause(UUID questId, long rowVersion, UUID actorId, String reason, String key) {
        NarrativeInvestigation parent = lock(questId);
        return pauseLocked(parent, rowVersion, actorId, reason, key);
    }

    @Transactional
    public NarrativeAdminDetailResponse resume(UUID questId, UUID actorId, String reason, String key) {
        NarrativeInvestigation parent = lock(questId);
        return resumeLocked(parent, null, actorId, reason, key);
    }

    private NarrativeAdminDetailResponse resumeLocked(NarrativeInvestigation parent, Long expectedRowVersion,
            UUID actorId, String reason, String key) {
        return idempotent(parent, "RESUME", null, expectedRowVersion, reason, key, () -> {
            requireParentVersion(parent, expectedRowVersion);
            NarrativeInvestigationVersion current = published(parent);
            requireApproved(current);
            if (parent.getRolloutState() != NarrativeRolloutState.PAUSED) {
                throw new BusinessRuleException("Only a paused narrative pilot can be resumed");
            }
            if (!checklist(current).stream().allMatch(NarrativeReadinessItem::passed)) {
                throw new BusinessRuleException("Published narrative version is not ready to resume");
            }
            long oldRowVersion = parent.getRowVersion();
            NarrativeRolloutState oldState = parent.getRolloutState();
            parent.setRolloutState(NarrativeRolloutState.ACTIVE);
            parent.setEnabled(true);
            bump(parent);
            auditRollout(actorId, parent, "RESUMED", reason, oldRowVersion, parent.getRowVersion(),
                    current.getVersion(), key, oldState, parent.getRolloutState());
            return detailOf(parent);
        });
    }

    @Transactional
    public NarrativeAdminDetailResponse resume(UUID questId, long rowVersion, UUID actorId, String reason, String key) {
        NarrativeInvestigation parent = lock(questId);
        return resumeLocked(parent, rowVersion, actorId, reason, key);
    }

    @Transactional
    public NarrativeAdminDetailResponse publishDraft(UUID questId, int version, UUID actorId, String reason, String key) {
        NarrativeInvestigation parent = lock(questId);
        return publishDraftLocked(parent, version, null, actorId, reason, key);
    }

    private NarrativeAdminDetailResponse publishDraftLocked(NarrativeInvestigation parent, int version,
            Long expectedRowVersion,
            UUID actorId, String reason, String key) {
        return idempotent(parent, "PUBLISH_DRAFT", version, expectedRowVersion, reason, key, () -> {
            requireParentVersion(parent, expectedRowVersion);
            published(parent);
            long oldRowVersion = parent.getRowVersion();
            NarrativeRolloutState oldState = parent.getRolloutState();
            NarrativeInvestigationVersion candidate = candidate(parent, version);
            close(candidate);
            publish(parent, candidate, parent.getRolloutState());
            auditRollout(actorId, parent, "DRAFT_PUBLISHED", reason, oldRowVersion, parent.getRowVersion(),
                    version, key, oldState, parent.getRolloutState());
            return detailOf(parent);
        });
    }

    @Transactional
    public NarrativeAdminDetailResponse publishDraft(UUID questId, int version, long rowVersion, UUID actorId, String reason, String key) {
        NarrativeInvestigation parent = lock(questId);
        return publishDraftLocked(parent, version, rowVersion, actorId, reason, key);
    }

    /* Legacy adapters: version resolution is explicit and published graph rows are never edited. */
    @Transactional(readOnly = true)
    public NarrativePilotAdminResponse readiness(UUID questId) { return listItem(find(questId)); }

    @Transactional(readOnly = true)
    public NarrativeDraftResponse draft(UUID questId) {
        NarrativeInvestigation parent = find(questId);
        NarrativeInvestigationVersion draft = openDraft(parent);
        return new NarrativeDraftResponse(questId, draft.getVersion(), draft.getGraphJson());
    }

    @Transactional
    public NarrativeDraftResponse updateDraft(UUID questId, UUID actorId, NarrativeDraftUpdateRequest request) {
        NarrativeInvestigation parent = lock(questId);
        NarrativeInvestigationVersion draft = openDraft(parent);
        if (draft.getVersion() != request.expectedVersion()) {
            throw conflict(ErrorCode.VERSION_CONFLICT, "Legacy draft request must name the open draft version");
        }
        NarrativeVersionResponse saved = patchDraft(questId, draft.getVersion(), actorId,
                new NarrativePatchDraftRequest(request.rowVersion(), null, request.graphJson(), request.reason()));
        return new NarrativeDraftResponse(questId, saved.version(), objectMapper.valueToTree(saved.graph()).toString());
    }

    @Transactional
    public NarrativePilotAdminResponse enable(UUID questId, UUID actorId, String reason) {
        NarrativeInvestigation parent = lock(questId);
        NarrativeInvestigationVersion draft = openDraft(parent);
        enable(questId, draft.getVersion(), actorId, reason, "legacy-enable-" + UUID.randomUUID());
        return listItem(find(questId));
    }

    @Transactional
    public NarrativePilotAdminResponse disable(UUID questId, UUID actorId, String reason) {
        pause(questId, actorId, reason, "legacy-pause-" + UUID.randomUUID());
        return listItem(find(questId));
    }

    private NarrativeAdminDetailResponse idempotent(NarrativeInvestigation parent, String action, Integer version,
            Long expectedRowVersion, String reason, String key, Supplier<NarrativeAdminDetailResponse> mutation) {
        if (key == null || key.isBlank()) throw new BusinessRuleException("Idempotency-Key is required");
        String normalizedReason = reason(reason, "Thao tác rollout từ Admin Console");
        String requestHash = hash(action + "|" + version + "|" + expectedRowVersion + "|" + normalizedReason);
        Optional<NarrativeRolloutIdempotency> prior =
                idempotency.findByInvestigationIdAndIdempotencyKey(parent.getId(), key.trim());
        if (prior.isPresent()) {
            if (!prior.get().getAction().equals(action) || !prior.get().getRequestHash().equals(requestHash)) {
                throw conflict(ErrorCode.IDEMPOTENCY_CONFLICT,
                        "Idempotency-Key was already used for a different rollout request");
            }
            try { return rolloutMapper().readValue(prior.get().getResponseJson(), NarrativeAdminDetailResponse.class); }
            catch (JsonProcessingException exception) { throw new BusinessRuleException("Stored rollout response cannot be read"); }
        }
        NarrativeAdminDetailResponse result = mutation.get();
        try {
            idempotency.save(NarrativeRolloutIdempotency.builder().investigationId(parent.getId())
                    .idempotencyKey(key.trim()).action(action).requestHash(requestHash)
                    .responseJson(rolloutMapper().writeValueAsString(result)).createdAt(Instant.now()).build());
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("Rollout response cannot be stored: " + exception.getOriginalMessage());
        }
        return result;
    }

    private NarrativeInvestigationVersion candidate(NarrativeInvestigation parent, int number) {
        NarrativeInvestigationVersion candidate = version(parent, number);
        if (!candidate.isOpenDraft()) throw new BusinessRuleException("Version is not the open draft");
        requireApproved(candidate);
        if (!locales(candidate.getApprovedLocalesJson()).contains("vi")) {
            throw new BusinessRuleException("Approved draft must include locale vi");
        }
        if (!checklist(candidate).stream().allMatch(NarrativeReadinessItem::passed)) {
            throw new BusinessRuleException("Narrative draft is not ready");
        }
        return candidate;
    }

    private void close(NarrativeInvestigationVersion candidate) {
        candidate.setOpenDraft(false);
        candidate.setUpdatedAt(Instant.now());
        versions.save(candidate);
    }

    private void publish(NarrativeInvestigation parent, NarrativeInvestigationVersion candidate, NarrativeRolloutState state) {
        parent.setPublishedVersionId(candidate.getId());
        parent.setRolloutState(state);
        parent.setEnabled(state == NarrativeRolloutState.ACTIVE);
        parent.setVersion(Math.max(parent.getVersion(), candidate.getVersion()));
        bump(parent);
    }

    private void bump(NarrativeInvestigation parent) {
        parent.setRowVersion(parent.getRowVersion() + 1);
        parent.setUpdatedAt(Instant.now());
        investigations.save(parent);
    }

    private NarrativeInvestigationVersion published(NarrativeInvestigation parent) {
        if (parent.getPublishedVersionId() == null) throw new BusinessRuleException("No published narrative version");
        return versions.findById(parent.getPublishedVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Published narrative version not found"));
    }

    private void requireApproved(NarrativeInvestigationVersion version) {
        if (!"APPROVED".equals(version.getReviewStatus()) || version.getReviewedAt() == null) {
            throw new BusinessRuleException("Narrative version is not approved");
        }
    }

    private List<NarrativeReadinessItem> checklist(NarrativeInvestigationVersion version) {
        List<NarrativeReadinessItem> checks = new ArrayList<>();
        checks.add(check("FEATURE_FLAG", pilotFeatures.isNarrativeInvestigation(), "Narrative feature flag must be enabled", null));
        checks.add(check("REVIEW_APPROVED", "APPROVED".equals(version.getReviewStatus()), "Draft must be approved", "reviewStatus"));
        checks.add(check("LOCALE_VI", locales(version.getApprovedLocalesJson()).contains("vi"), "Required locale vi must be approved", "approvedLocales"));
        try {
            JsonNode root = objectMapper.readTree(version.getGraphJson());
            List<String> errors = graphValidator.validate(objectMapper.treeToValue(root, NarrativeQuestGraph.class));
            checks.add(check("GRAPH_VALID", errors.isEmpty(), errors.isEmpty() ? "Graph is structurally valid" : String.join("; ", errors), "graphJson"));
            JsonNode sources = root.get("sources");
            checks.add(check("SOURCE_PRESENT", sources != null && sources.isArray() && !sources.isEmpty(), "Graph must include at least one source", "sources"));
        } catch (JsonProcessingException exception) {
            checks.add(check("GRAPH_VALID", false, "Graph JSON cannot be read", "graphJson"));
            checks.add(check("SOURCE_PRESENT", false, "Graph must include at least one source", "sources"));
        }
        return checks;
    }

    private NarrativeReadinessItem check(String code, boolean passed, String message, String field) {
        return new NarrativeReadinessItem(code, passed, message, null, field);
    }

    private void validateGraph(String graphJson) {
        try {
            JsonNode root = objectMapper.readTree(graphJson);
            List<String> errors = graphValidator.validate(objectMapper.treeToValue(root, NarrativeQuestGraph.class));
            if (!errors.isEmpty()) throw new BusinessRuleException("Invalid narrative graph: " + String.join("; ", errors));
            JsonNode sources = root.get("sources");
            if (sources == null || !sources.isArray() || sources.isEmpty()) {
                throw new BusinessRuleException("Narrative graph must include at least one source");
            }
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("Narrative graph JSON cannot be read");
        }
    }

    private NarrativeAdminDetailResponse detailOf(NarrativeInvestigation parent) {
        Quest quest = quests.findById(parent.getQuestId()).orElseThrow(() -> new ResourceNotFoundException("Quest not found"));
        List<NarrativeInvestigationVersion> all = versions.findByInvestigationIdOrderByVersionDesc(parent.getId());
        Integer published = all.stream().filter(v -> v.getId().equals(parent.getPublishedVersionId()))
                .map(NarrativeInvestigationVersion::getVersion).findFirst().orElse(null);
        Integer draft = all.stream().filter(NarrativeInvestigationVersion::isOpenDraft)
                .map(NarrativeInvestigationVersion::getVersion).findFirst().orElse(null);
        NarrativeInvestigationVersion publishedEntity = all.stream()
                .filter(v -> v.getId().equals(parent.getPublishedVersionId())).findFirst().orElse(null);
        NarrativeInvestigationVersion draftEntity = all.stream()
                .filter(NarrativeInvestigationVersion::isOpenDraft).findFirst().orElse(null);
        NarrativeInvestigationVersion shown = draftEntity != null ? draftEntity : publishedEntity;
        return new NarrativeAdminDetailResponse(parent.getQuestId(), quest.getTitle(), parent.getId(),
                quest.getLocationId(), null, shown == null ? "DRAFT" : shown.getReviewStatus(),
                parent.getRolloutState(), parent.getRowVersion(), published, draft,
                draftEntity == null ? null : draftEntity.getReviewStatus(), pilotFeatures.isNarrativeInvestigation(),
                publishedEntity == null ? null : response(publishedEntity),
                draftEntity == null ? null : response(draftEntity), all.stream().map(this::response).toList());
    }

    private NarrativePilotListResponse pageItem(NarrativeInvestigation parent) {
        Quest quest = quests.findById(parent.getQuestId()).orElseThrow(() -> new ResourceNotFoundException("Quest not found"));
        NarrativeInvestigationVersion draft = versions.findByInvestigationIdAndOpenDraftTrue(parent.getId()).orElse(null);
        NarrativeInvestigationVersion published = parent.getPublishedVersionId() == null ? null
                : versions.findById(parent.getPublishedVersionId()).orElse(null);
        NarrativeInvestigationVersion shown = draft != null ? draft : published;
        return new NarrativePilotListResponse(parent.getQuestId(), quest.getTitle(), parent.getId(), quest.getLocationId(),
                null, shown == null ? "DRAFT" : shown.getReviewStatus(), parent.getRolloutState(),
                published == null ? null : published.getVersion(), draft == null ? null : draft.getVersion(),
                draft == null ? null : draft.getReviewStatus(), parent.getRowVersion(),
                pilotFeatures.isNarrativeInvestigation());
    }

    private NarrativePilotAdminResponse listItem(NarrativeInvestigation parent) {
        Quest quest = quests.findById(parent.getQuestId()).orElseThrow(() -> new ResourceNotFoundException("Quest not found"));
        NarrativeInvestigationVersion shown = versions.findByInvestigationIdAndOpenDraftTrue(parent.getId())
                .orElseGet(() -> parent.getPublishedVersionId() == null ? null : versions.findById(parent.getPublishedVersionId()).orElse(null));
        if (shown == null) return new NarrativePilotAdminResponse(parent.getQuestId(), quest.getTitle(), parent.getId(),
                parent.getVersion(), false, "DRAFT", Set.of(), null, null, pilotFeatures.isNarrativeInvestigation(),
                false, 0, List.of("No narrative version"));
        List<NarrativeReadinessItem> checks = checklist(shown);
        return new NarrativePilotAdminResponse(parent.getQuestId(), quest.getTitle(), parent.getId(), shown.getVersion(),
                parent.getRolloutState() == NarrativeRolloutState.ACTIVE, shown.getReviewStatus(), locales(shown.getApprovedLocalesJson()),
                shown.getReviewedBy(), shown.getReviewedAt(), pilotFeatures.isNarrativeInvestigation(),
                checks.stream().filter(c -> c.code().equals("GRAPH_VALID")).allMatch(NarrativeReadinessItem::passed),
                sourceCount(shown.getGraphJson()), checks.stream().filter(c -> !c.passed()).map(NarrativeReadinessItem::message).toList());
    }

    private int sourceCount(String graph) {
        try { JsonNode sources = objectMapper.readTree(graph).get("sources"); return sources != null && sources.isArray() ? sources.size() : 0; }
        catch (JsonProcessingException exception) { return 0; }
    }

    private NarrativeVersionResponse response(NarrativeInvestigationVersion version) {
        return new NarrativeVersionResponse(version.getId(), version.getVersion(), version.getRevision(), version.getRevision(),
                version.isOpenDraft(), version.isOpenDraft() ? "DRAFT" : "PUBLISHED", readGraph(version.getGraphJson()),
                version.getReviewStatus(), locales(version.getApprovedLocalesJson()), version.getReviewedBy(),
                version.getReviewedAt(), version.getCreatedAt(), version.getUpdatedAt(), true, null);
    }

    private NarrativeQuestGraph readGraph(String graphJson) {
        try { return objectMapper.readValue(graphJson, NarrativeQuestGraph.class); }
        catch (JsonProcessingException exception) { throw new BusinessRuleException("Narrative graph JSON cannot be read"); }
    }

    private String graphJson(JsonNode graph, String legacyGraphJson) {
        if (graph != null && !graph.isNull()) return graph.toString();
        return legacyGraphJson;
    }

    private String reason(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void requireParentVersion(NarrativeInvestigation parent, Long expected) {
        if (expected != null && parent.getRowVersion() != expected) {
            throw conflict(ErrorCode.VERSION_CONFLICT, "Narrative investigation was changed by another administrator");
        }
    }

    private NarrativeInvestigation find(UUID questId) {
        return investigations.findByQuestId(questId).orElseThrow(() -> new ResourceNotFoundException("Narrative investigation not found"));
    }

    private NarrativeInvestigation lock(UUID questId) {
        return investigations.findForUpdateByQuestId(questId).orElseThrow(() -> new ResourceNotFoundException("Narrative investigation not found"));
    }

    private NarrativeInvestigationVersion version(NarrativeInvestigation parent, int number) {
        return versions.findByInvestigationIdAndVersion(parent.getId(), number)
                .orElseThrow(() -> new ResourceNotFoundException("Narrative version not found"));
    }

    private NarrativeInvestigationVersion openDraft(NarrativeInvestigation parent) {
        return versions.findByInvestigationIdAndOpenDraftTrue(parent.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Open narrative draft not found"));
    }

    private void audit(UUID actorId, NarrativeInvestigation parent, String action, String reason,
            Long oldRevision, Long newRevision, Integer version, String key) {
        audit.append(new AdminAuditCommand(actorId, "NARRATIVE_INVESTIGATION", parent.getId(), action,
                oldRevision, newRevision, reason.trim(), key,
                Map.of("questId", parent.getQuestId().toString(), "version", version == null ? 0 : version)));
    }

    private void auditRollout(UUID actorId, NarrativeInvestigation parent, String action, String reason,
            long oldRowVersion, long newRowVersion, Integer version, String key,
            NarrativeRolloutState fromState, NarrativeRolloutState toState) {
        audit.append(new AdminAuditCommand(actorId, "NARRATIVE_INVESTIGATION", parent.getId(), action,
                oldRowVersion, newRowVersion, reason.trim(), key,
                Map.of("questId", parent.getQuestId().toString(), "version", version == null ? 0 : version,
                        "fromState", fromState.name(), "toState", toState.name())));
    }

    private Set<String> locales(String json) {
        try { return json == null ? Set.of() : objectMapper.readValue(json, new TypeReference<Set<String>>() {}); }
        catch (JsonProcessingException exception) { return Set.of(); }
    }

    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    private ObjectMapper rolloutMapper() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Instant.class, new JsonSerializer<>() {
            @Override public void serialize(Instant value, JsonGenerator generator, SerializerProvider serializers)
                    throws IOException { generator.writeString(value.toString()); }
        });
        module.addDeserializer(Instant.class, new JsonDeserializer<>() {
            @Override public Instant deserialize(JsonParser parser, DeserializationContext context)
                    throws IOException { return Instant.parse(parser.getValueAsString()); }
        });
        return objectMapper.copy().registerModule(module);
    }

    private NarrativeConflictException conflict(ErrorCode code, String message) { return new NarrativeConflictException(code, message); }
}

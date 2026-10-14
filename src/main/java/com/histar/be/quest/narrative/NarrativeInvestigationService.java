package com.histar.be.quest.narrative;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ErrorCode;
import com.histar.be.common.exception.NarrativeConflictException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.quest.narrative.entity.NarrativeInvestigation;
import com.histar.be.quest.narrative.entity.NarrativeInvestigationVersion;
import com.histar.be.quest.narrative.entity.NarrativeRolloutState;
import com.histar.be.quest.narrative.model.NarrativeQuestGraph;
import java.util.UUID;
import java.util.*;
import com.histar.be.quest.narrative.entity.UserNarrativeInvestigationProgress;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.minigame.UserGameResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NarrativeInvestigationService {
    private final NarrativeInvestigationRepository repository;
    private final NarrativeInvestigationVersionRepository versionRepository;
    private final ObjectMapper objectMapper;
    private final UserNarrativeInvestigationProgressRepository progressRepository;
    private final NarrativeRewardService rewardService;
    private final ProfileRepository profileRepository;
    private final UserGameResultRepository minigameResults;

    @Autowired
    public NarrativeInvestigationService(NarrativeInvestigationRepository repository,
                                         NarrativeInvestigationVersionRepository versionRepository,
                                         ObjectMapper objectMapper,
                                         UserNarrativeInvestigationProgressRepository progressRepository,
                                         NarrativeRewardService rewardService,
                                         ProfileRepository profileRepository,
                                         UserGameResultRepository minigameResults) {
        this.repository = repository;
        this.versionRepository = versionRepository;
        this.objectMapper = objectMapper;
        this.progressRepository = progressRepository;
        this.rewardService = rewardService;
        this.profileRepository = profileRepository;
        this.minigameResults = minigameResults;
    }

    /** Compatibility constructor for focused unit tests and older integrations. */
    public NarrativeInvestigationService(NarrativeInvestigationRepository repository,
                                         ObjectMapper objectMapper,
                                         UserNarrativeInvestigationProgressRepository progressRepository,
                                         NarrativeRewardService rewardService,
                                         ProfileRepository profileRepository) {
        this(repository, null, objectMapper, progressRepository, rewardService, profileRepository, null);
    }

    /** Compatibility constructor for tests that exercise minigame behavior without version storage. */
    public NarrativeInvestigationService(NarrativeInvestigationRepository repository,
                                         ObjectMapper objectMapper,
                                         UserNarrativeInvestigationProgressRepository progressRepository,
                                         NarrativeRewardService rewardService,
                                         ProfileRepository profileRepository,
                                         UserGameResultRepository minigameResults) {
        this(repository, null, objectMapper, progressRepository, rewardService, profileRepository, minigameResults);
    }

    @Transactional(readOnly = true)
    public NarrativeInvestigationResponse forQuest(UUID questId) {
        if (versionRepository == null) {
            NarrativeInvestigation investigation = repository.findByQuestIdAndEnabledTrue(questId)
                    .orElseThrow(() -> new ResourceNotFoundException("Narrative investigation not found"));
            return parse(investigation);
        }
        NarrativeInvestigation investigation = repository.findByQuestId(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Narrative investigation not found"));
        if (investigation.getRolloutState() == NarrativeRolloutState.NOT_RELEASED
                || investigation.getPublishedVersionId() == null) {
            throw new ResourceNotFoundException("Narrative investigation is not released");
        }
        return parse(investigation, publishedVersion(investigation));
    }

    private NarrativeInvestigationResponse parse(NarrativeInvestigation investigation) {
        if (!"APPROVED".equals(investigation.getReviewStatus()) || investigation.getReviewedAt() == null
                || investigation.getApprovedLocalesJson() == null || investigation.getApprovedLocalesJson().isBlank()
                || "[]".equals(investigation.getApprovedLocalesJson().trim())) {
            throw new ResourceNotFoundException("Narrative investigation is not approved for pilot");
        }
        try {
            return new NarrativeInvestigationResponse(
                    investigation.getQuestId(), investigation.getVersion(),
                    objectMapper.readValue(investigation.getGraphJson(), NarrativeQuestGraph.class));
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("Narrative investigation graph không đọc được");
        }
    }

    private NarrativeInvestigationResponse parse(
            NarrativeInvestigation investigation, NarrativeInvestigationVersion version) {
        if (!"APPROVED".equals(version.getReviewStatus()) || version.getReviewedAt() == null
                || version.getApprovedLocalesJson() == null
                || !readLocales(version.getApprovedLocalesJson()).contains("vi")) {
            throw new ResourceNotFoundException("Narrative investigation version is not approved for pilot");
        }
        try {
            return new NarrativeInvestigationResponse(investigation.getQuestId(), version.getVersion(),
                    objectMapper.readValue(version.getGraphJson(), NarrativeQuestGraph.class));
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("Narrative investigation graph không đọc được");
        }
    }

    @Transactional
    public NarrativeProgressResponse progress(UUID userId, UUID questId) {
        if (versionRepository == null) {
            NarrativeInvestigationResponse investigation = forQuest(questId);
            NarrativeInvestigation entity = repository.findByQuestIdAndEnabledTrue(questId).orElseThrow();
            NarrativeProgressState state = progressRepository.findByUserIdAndInvestigationId(userId, entity.getId())
                    .map(this::read).filter(p -> p.version() == investigation.version())
                    .orElseGet(() -> NarrativeProgressState.start(investigation.version(), investigation.graph().startNodeCode()));
            return new NarrativeProgressResponse(investigation, state);
        }
        NarrativeInvestigation entity = repository.findByQuestId(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Narrative investigation not found"));
        if (entity.getPublishedVersionId() == null) {
            throw new ResourceNotFoundException("Narrative investigation is not released");
        }
        Optional<UserNarrativeInvestigationProgress> stored =
                progressRepository.findByUserIdAndInvestigationId(userId, entity.getId());
        NarrativeInvestigationVersion bound = stored.isPresent() && stored.get().getInvestigationVersion() != null
                ? historicalVersion(entity, stored.get().getInvestigationVersion()) : publishedVersion(entity);
        NarrativeInvestigationResponse investigation = parse(entity, bound);
        NarrativeProgressState state = stored.map(this::read)
                .orElseGet(() -> NarrativeProgressState.start(investigation.version(), investigation.graph().startNodeCode()));
        return new NarrativeProgressResponse(investigation, state);
    }

    @Transactional
    public NarrativeProgressResponse act(UUID userId, UUID questId, NarrativeActionRequest request) {
        // Lock a parent row that always exists. Locking only progress leaves a first-action
        // race when no progress row has been created yet.
        profileRepository.findByIdForUpdate(userId).orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        NarrativeInvestigation entity;
        if (versionRepository == null) {
            entity = repository.findByQuestIdAndEnabledTrue(questId)
                    .orElseThrow(() -> new ResourceNotFoundException("Narrative investigation not found"));
        } else {
            entity = repository.findForUpdateByQuestId(questId)
                    .orElseGet(() -> repository.findByQuestId(questId)
                            .orElseThrow(() -> new ResourceNotFoundException("Narrative investigation not found")));
            if (entity.getRolloutState() == NarrativeRolloutState.PAUSED) {
                throw new BusinessRuleException("Narrative investigation is paused");
            }
            if (entity.getRolloutState() != NarrativeRolloutState.ACTIVE || entity.getPublishedVersionId() == null) {
                throw new ResourceNotFoundException("Narrative investigation is not active");
            }
        }
        UserNarrativeInvestigationProgress row = progressRepository.findWithLockByUserIdAndInvestigationId(userId, entity.getId())
                .orElseGet(() -> UserNarrativeInvestigationProgress.builder().userId(userId).investigationId(entity.getId()).build());
        NarrativeInvestigationResponse investigation = versionRepository == null
                ? parse(entity)
                : parse(entity, row.getId() != null && row.getInvestigationVersion() != null
                        ? historicalVersion(entity, row.getInvestigationVersion()) : publishedVersion(entity));
        NarrativeProgressState prior = row.getId() == null || row.getInvestigationVersion() == null
                ? NarrativeProgressState.start(investigation.version(), investigation.graph().startNodeCode()) : read(row);
        NarrativeProgressResponse current = new NarrativeProgressResponse(investigation, prior); NarrativeQuestGraph graph = current.investigation().graph();
        if (request.investigationVersion() != current.investigation().version()) throw new BusinessRuleException("Phiên bản hồ sơ đã thay đổi; hãy tải lại nhiệm vụ.");
        Map<String, com.histar.be.quest.narrative.model.NarrativeNode> nodes = new HashMap<>(); for (var n: graph.nodes()) nodes.put(n.code(), n);
        var target = nodes.get(request.nodeCode()); if (target == null) throw new BusinessRuleException("Node không hợp lệ");
        NarrativeProgressState s = current.progress(); Set<String> visited = new LinkedHashSet<>(s.visitedNodeCodes()); Set<String> observations = new LinkedHashSet<>(s.observationCodes()); Set<String> chatNodes = new LinkedHashSet<>(s.chatNodeCodes()); Set<String> evidence = new LinkedHashSet<>(s.evidenceCodes()); String outcome = s.selectedOutcome();
        var currentNode = nodes.get(visited.stream().reduce((first, second) -> second).orElse(graph.startNodeCode()));
        boolean reachable = visited.contains(request.nodeCode()) || visited.stream().map(nodes::get).filter(Objects::nonNull).flatMap(n -> (n.choices()==null?List.<com.histar.be.quest.narrative.model.NarrativeChoice>of():n.choices()).stream()).anyMatch(c -> request.nodeCode().equals(c.targetNodeCode()));
        if ((request.action() == NarrativeActionType.VISIT_NODE || request.action() == NarrativeActionType.SELECT_OUTCOME) && !reachable) throw new BusinessRuleException("Chuyển nhánh không hợp lệ");
        if (request.action() == NarrativeActionType.RECORD_OBSERVATION) {
            if (!visited.contains(request.nodeCode()) || !"TOUR_360_OBSERVATION".equals(target.type())) throw new BusinessRuleException("Chỉ có thể ghi nhận quan sát ở node Tour 360");
            if (request.observationCode() == null || request.observationCode().isBlank() || target.observationChecklist() == null || !target.observationChecklist().contains(request.observationCode())) throw new BusinessRuleException("Mục quan sát không hợp lệ");
            observations.add(request.observationCode());
        }
        if (request.action() == NarrativeActionType.RECORD_CHAT_INTERACTION) {
            if (!visited.contains(request.nodeCode()) || !"AI_INTERVIEW".equals(target.type())) throw new BusinessRuleException("Chỉ có thể đồng bộ câu hỏi ở node phỏng vấn Chrono");
            chatNodes.add(request.nodeCode());
        }
        if (request.action() == NarrativeActionType.VISIT_NODE && currentNode != null && "EVIDENCE".equals(currentNode.type()) && !evidence.contains(currentNode.code())) throw new BusinessRuleException("Hãy ghi nhận mảnh chứng cứ hiện tại trước khi đi tiếp");
        if (request.action() == NarrativeActionType.COLLECT_EVIDENCE) {
            if (!graph.evidenceNodeCodes().contains(request.nodeCode()) || !visited.contains(request.nodeCode())) throw new BusinessRuleException("Chứng cứ chưa được xác minh");
            if (target.linkedMinigameId() != null && !target.linkedMinigameId().isBlank()) {
                UUID minigameId;
                try { minigameId = UUID.fromString(target.linkedMinigameId()); }
                catch (IllegalArgumentException ex) { throw new BusinessRuleException("Mini game liên kết không hợp lệ"); }
                if (minigameResults == null || minigameResults.findByUserIdAndGameId(userId, minigameId).map(r -> r.getBestStars() >= 1).orElse(false) == false) {
                    throw new BusinessRuleException("Hãy hoàn thành mini-game Soi nắp trước khi ghi nhận chứng cứ");
                }
            }
            evidence.add(request.nodeCode()); rewardService.awardOnce(userId, questId, "NARRATIVE_EVIDENCE", request.nodeCode(), target.rewardXp() == null ? 10 : target.rewardXp());
        }
        if (request.action() == NarrativeActionType.SELECT_OUTCOME) { if (!graph.outcomeNodeCodes().contains(request.nodeCode()) || evidence.size() < Optional.ofNullable(target.minimumEvidence()).orElse(0)) throw new BusinessRuleException("Chưa đủ chứng cứ để kết luận"); outcome=request.nodeCode(); rewardService.awardOnce(userId,questId,"NARRATIVE_OUTCOME",request.nodeCode(),20); }
        visited.add(request.nodeCode()); NarrativeProgressState next = new NarrativeProgressState(current.investigation().version(), visited, observations, chatNodes, evidence, outcome);
        write(row, next); row.setUpdatedAt(java.time.Instant.now()); progressRepository.save(row);
        return new NarrativeProgressResponse(current.investigation(),next);
    }

    @Transactional
    public NarrativeProgressResponse recordChatInteraction(UUID userId, UUID questId, String nodeCode, int version) {
        return act(userId, questId, new NarrativeActionRequest(
                NarrativeActionType.RECORD_CHAT_INTERACTION, nodeCode, null, version));
    }
    private NarrativeProgressState read(UserNarrativeInvestigationProgress row){
        if (row.getInvestigationVersion() != null && row.getVisitedNodeCodesJson() != null && row.getEvidenceCodesJson() != null) {
            try {
                return new NarrativeProgressState(row.getInvestigationVersion(),
                        new LinkedHashSet<>(objectMapper.readValue(row.getVisitedNodeCodesJson(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})),
                        row.getObservationCodesJson() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(objectMapper.readValue(row.getObservationCodesJson(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})),
                        row.getChatNodeCodesJson() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(objectMapper.readValue(row.getChatNodeCodesJson(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})),
                        new LinkedHashSet<>(objectMapper.readValue(row.getEvidenceCodesJson(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})),
                        row.getSelectedOutcomeCode());
            } catch (Exception e) { throw new BusinessRuleException("Tiến độ điều tra không đọc được"); }
        }
        try {
            NarrativeProgressState legacy = objectMapper.readValue(row.getStateJson(), NarrativeProgressState.class);
            return new NarrativeProgressState(legacy.version(), legacy.visitedNodeCodes(),
                    legacy.observationCodes() == null ? new LinkedHashSet<>() : legacy.observationCodes(),
                    legacy.chatNodeCodes() == null ? new LinkedHashSet<>() : legacy.chatNodeCodes(),
                    legacy.evidenceCodes() == null ? new LinkedHashSet<>() : legacy.evidenceCodes(), legacy.selectedOutcome());
        } catch(Exception e){throw new BusinessRuleException("Tiến độ điều tra không đọc được");}
    }
    private void write(UserNarrativeInvestigationProgress row, NarrativeProgressState state) {
        try {
            row.setStateJson(objectMapper.writeValueAsString(state));
            row.setInvestigationVersion(state.version());
            row.setVisitedNodeCodesJson(objectMapper.writeValueAsString(state.visitedNodeCodes()));
            row.setObservationCodesJson(objectMapper.writeValueAsString(state.observationCodes()));
            row.setChatNodeCodesJson(objectMapper.writeValueAsString(state.chatNodeCodes()));
            row.setEvidenceCodesJson(objectMapper.writeValueAsString(state.evidenceCodes()));
            row.setSelectedOutcomeCode(state.selectedOutcome());
        } catch (JsonProcessingException e) { throw new BusinessRuleException("Không thể lưu tiến độ"); }
    }

    private NarrativeInvestigationVersion publishedVersion(NarrativeInvestigation investigation) {
        return versionRepository.findById(investigation.getPublishedVersionId())
                .filter(version -> version.getInvestigationId().equals(investigation.getId()))
                .orElseThrow(() -> cannotContinue("Published narrative version is missing"));
    }

    private NarrativeInvestigationVersion historicalVersion(NarrativeInvestigation investigation, int version) {
        return versionRepository.findByInvestigationIdAndVersion(investigation.getId(), version)
                .orElseThrow(() -> cannotContinue("Historical narrative version " + version + " is missing; progress cannot continue"));
    }

    private NarrativeConflictException cannotContinue(String message) {
        return new NarrativeConflictException(ErrorCode.NARRATIVE_CANNOT_CONTINUE, message);
    }

    private Set<String> readLocales(String json) {
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Set<String>>() {});
        } catch (JsonProcessingException exception) {
            return Set.of();
        }
    }
}

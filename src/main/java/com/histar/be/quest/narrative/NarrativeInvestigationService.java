package com.histar.be.quest.narrative;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.quest.narrative.entity.NarrativeInvestigation;
import com.histar.be.quest.narrative.model.NarrativeQuestGraph;
import java.util.UUID;
import java.util.*;
import com.histar.be.quest.narrative.entity.UserNarrativeInvestigationProgress;
import com.histar.be.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NarrativeInvestigationService {
    private final NarrativeInvestigationRepository repository;
    private final ObjectMapper objectMapper;
    private final UserNarrativeInvestigationProgressRepository progressRepository;
    private final NarrativeRewardService rewardService;
    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public NarrativeInvestigationResponse forQuest(UUID questId) {
        NarrativeInvestigation investigation = repository.findByQuestIdAndEnabledTrue(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Narrative investigation not found"));
        return parse(investigation);
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

    @Transactional
    public NarrativeProgressResponse progress(UUID userId, UUID questId) {
        NarrativeInvestigationResponse investigation = forQuest(questId);
        NarrativeInvestigation entity = repository.findByQuestIdAndEnabledTrue(questId).orElseThrow();
        NarrativeProgressState state = progressRepository.findByUserIdAndInvestigationId(userId, entity.getId())
                .map(this::read).filter(p -> p.version() == investigation.version())
                .orElseGet(() -> NarrativeProgressState.start(investigation.version(), investigation.graph().startNodeCode()));
        return new NarrativeProgressResponse(investigation, state);
    }

    @Transactional
    public NarrativeProgressResponse act(UUID userId, UUID questId, NarrativeActionRequest request) {
        // Lock a parent row that always exists. Locking only progress leaves a first-action
        // race when no progress row has been created yet.
        profileRepository.findByIdForUpdate(userId).orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        NarrativeInvestigation entity = repository.findByQuestIdAndEnabledTrue(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Narrative investigation not found"));
        NarrativeInvestigationResponse investigation = parse(entity);
        UserNarrativeInvestigationProgress row = progressRepository.findWithLockByUserIdAndInvestigationId(userId, entity.getId())
                .orElseGet(() -> UserNarrativeInvestigationProgress.builder().userId(userId).investigationId(entity.getId()).build());
        NarrativeProgressState prior = row.getId() == null || row.getInvestigationVersion() == null
                ? NarrativeProgressState.start(investigation.version(), investigation.graph().startNodeCode()) : read(row);
        if (prior.version() != investigation.version()) {
            prior = NarrativeProgressState.start(investigation.version(), investigation.graph().startNodeCode());
        }
        NarrativeProgressResponse current = new NarrativeProgressResponse(investigation, prior); NarrativeQuestGraph graph = current.investigation().graph();
        if (request.investigationVersion() != current.investigation().version()) throw new BusinessRuleException("Phiên bản hồ sơ đã thay đổi; hãy tải lại nhiệm vụ.");
        Map<String, com.histar.be.quest.narrative.model.NarrativeNode> nodes = new HashMap<>(); for (var n: graph.nodes()) nodes.put(n.code(), n);
        var target = nodes.get(request.nodeCode()); if (target == null) throw new BusinessRuleException("Node không hợp lệ");
        NarrativeProgressState s = current.progress(); Set<String> visited = new LinkedHashSet<>(s.visitedNodeCodes()); Set<String> evidence = new LinkedHashSet<>(s.evidenceCodes()); String outcome = s.selectedOutcome();
        boolean reachable = visited.contains(request.nodeCode()) || visited.stream().map(nodes::get).filter(Objects::nonNull).flatMap(n -> (n.choices()==null?List.<com.histar.be.quest.narrative.model.NarrativeChoice>of():n.choices()).stream()).anyMatch(c -> request.nodeCode().equals(c.targetNodeCode()));
        if (request.action() == NarrativeActionType.VISIT_NODE && !reachable) throw new BusinessRuleException("Chuyển nhánh không hợp lệ");
        if (request.action() == NarrativeActionType.COLLECT_EVIDENCE) { if (!graph.evidenceNodeCodes().contains(request.nodeCode()) || !visited.contains(request.nodeCode())) throw new BusinessRuleException("Chứng cứ chưa được xác minh"); evidence.add(request.nodeCode()); rewardService.awardOnce(userId,questId,"NARRATIVE_EVIDENCE",request.nodeCode(),10); }
        if (request.action() == NarrativeActionType.SELECT_OUTCOME) { if (!graph.outcomeNodeCodes().contains(request.nodeCode()) || evidence.size() < Optional.ofNullable(target.minimumEvidence()).orElse(0)) throw new BusinessRuleException("Chưa đủ chứng cứ để kết luận"); outcome=request.nodeCode(); rewardService.awardOnce(userId,questId,"NARRATIVE_OUTCOME",request.nodeCode(),20); }
        visited.add(request.nodeCode()); NarrativeProgressState next = new NarrativeProgressState(current.investigation().version(), visited, evidence, outcome);
        write(row, next); row.setUpdatedAt(java.time.Instant.now()); progressRepository.save(row);
        return new NarrativeProgressResponse(current.investigation(),next);
    }
    private NarrativeProgressState read(UserNarrativeInvestigationProgress row){
        if (row.getInvestigationVersion() != null && row.getVisitedNodeCodesJson() != null && row.getEvidenceCodesJson() != null) {
            try {
                return new NarrativeProgressState(row.getInvestigationVersion(),
                        new LinkedHashSet<>(objectMapper.readValue(row.getVisitedNodeCodesJson(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})),
                        new LinkedHashSet<>(objectMapper.readValue(row.getEvidenceCodesJson(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})),
                        row.getSelectedOutcomeCode());
            } catch (Exception e) { throw new BusinessRuleException("Tiến độ điều tra không đọc được"); }
        }
        try{return objectMapper.readValue(row.getStateJson(),NarrativeProgressState.class);}catch(Exception e){throw new BusinessRuleException("Tiến độ điều tra không đọc được");}
    }
    private void write(UserNarrativeInvestigationProgress row, NarrativeProgressState state) {
        try {
            row.setStateJson(objectMapper.writeValueAsString(state));
            row.setInvestigationVersion(state.version());
            row.setVisitedNodeCodesJson(objectMapper.writeValueAsString(state.visitedNodeCodes()));
            row.setEvidenceCodesJson(objectMapper.writeValueAsString(state.evidenceCodes()));
            row.setSelectedOutcomeCode(state.selectedOutcome());
        } catch (JsonProcessingException e) { throw new BusinessRuleException("Không thể lưu tiến độ"); }
    }
}

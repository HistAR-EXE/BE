package com.histar.be.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.artifact.entity.Artifact;
import com.histar.be.artifact.repository.ArtifactRepository;
import com.histar.be.discovery.bridge.entity.ArtifactUnlockRequirement;
import com.histar.be.discovery.bridge.entity.DiscoveryArtifactLink;
import com.histar.be.discovery.bridge.repository.ArtifactUnlockRequirementRepository;
import com.histar.be.discovery.bridge.repository.DiscoveryArtifactLinkRepository;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.gamification.rules.entity.UnlockRule;
import com.histar.be.gamification.rules.repository.UnlockRuleRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.entity.QuestStep;
import com.histar.be.quest.minigame.QuestMinigame;
import com.histar.be.quest.minigame.QuestMinigameRepository;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.repository.QuestStepRepository;
import com.histar.be.quest.service.impl.QuestStepProgressServiceImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeedConsistencyValidator {

    private static final Set<String> VALID_MINIGAME_ENGINES = Set.of(
            "xep_tang", "soi_nap", "dao_gap", "san_co_vat", "duong_khoi", "im_lang", "phong_van");

    private final QuestRepository questRepository;
    private final QuestStepRepository questStepRepository;
    private final QuestMinigameRepository questMinigameRepository;
    private final DiscoveryPointRepository discoveryPointRepository;
    private final ArtifactRepository artifactRepository;
    private final ArtifactUnlockRequirementRepository artifactUnlockRequirementRepository;
    private final UnlockRuleRepository unlockRuleRepository;
    private final DiscoveryArtifactLinkRepository discoveryArtifactLinkRepository;
    private final ObjectMapper objectMapper;

    public void validateAll() {
        validateMinigameConfigs();
        validateQuestStepRecords();
        validateQuestSteps();
        validateArtifactRequirements();
        validateUnlockRules();
        validateDiscoveryArtifactLinks();
        detectArtifactCycles();
    }

    private void validateQuestStepRecords() {
        for (Quest quest : questRepository.findAll()) {
            validateQuestDependencies(quest);
            if (quest.getStepsTotal() == null) {
                continue;
            }
            int actualSteps = questStepRepository.findByQuestId(quest.getId()).size();
            if (actualSteps == 0) {
                throw new SeedConsistencyException("Quest " + quest.getId() + " has no quest_steps");
            }
            if (!quest.getStepsTotal().equals(actualSteps)) {
                throw new SeedConsistencyException("Quest " + quest.getId() + " steps_total=" + quest.getStepsTotal()
                        + " differs from quest_steps=" + actualSteps);
            }
            List<QuestStep> steps = questStepRepository.findByQuestId(quest.getId());
            validateQuestStepActions(quest, steps);
            if (Boolean.TRUE.equals(quest.getRequireOnsiteCheckin())
                    && steps.stream().noneMatch(step -> "checkin".equals(step.getActionType()))) {
                throw new SeedConsistencyException("Onsite quest " + quest.getId() + " is missing a check-in step");
            }
        }
    }

    private void validateQuestDependencies(Quest quest) {
        if (quest.getUnlockAfterQuestId() != null && !questRepository.existsById(quest.getUnlockAfterQuestId())) {
            throw new SeedConsistencyException("Quest " + quest.getId()
                    + " depends on missing quest: " + quest.getUnlockAfterQuestId());
        }
        if (quest.getUnlockDiscoveryKeys() == null || quest.getUnlockDiscoveryKeys().isBlank()) {
            return;
        }
        for (String unlockKey : QuestStepProgressServiceImpl.parseSteps(quest.getUnlockDiscoveryKeys())) {
            if (!hasBackendUnlockKey(quest.getLocationId(), unlockKey)) {
                throw new SeedConsistencyException("Quest " + quest.getId()
                        + " depends on missing discovery key: " + unlockKey);
            }
        }
    }

    private void validateQuestStepActions(Quest quest, List<QuestStep> steps) {
        Set<String> unlockKeys = new HashSet<>();
        for (QuestStep step : steps) {
            String unlockKey = step.getUnlockKey() == null ? "" : step.getUnlockKey().trim();
            if (unlockKey.isBlank()) {
                throw new SeedConsistencyException("Quest step " + step.getId() + " has an empty unlock_key");
            }
            if (!unlockKeys.add(unlockKey)) {
                throw new SeedConsistencyException("Quest " + quest.getId() + " has duplicate unlock_key: " + unlockKey);
            }
            String actionType = step.getActionType() == null ? "" : step.getActionType().trim();
            if ("dialogue".equals(actionType)
                    && (step.getChatPrompt() == null || step.getChatPrompt().isBlank())
                    && !hasBackendUnlockKey(quest.getLocationId(), unlockKey)) {
                throw new SeedConsistencyException("Dialogue step " + step.getId()
                        + " needs chat_prompt or a backend unlock_key: " + unlockKey);
            }
            if ("portal".equals(actionType) && step.getPortalEra() == null) {
                throw new SeedConsistencyException("Portal step " + step.getId() + " is missing portal_era");
            }
            if ("minigame".equals(actionType)) {
                if (step.getMinigameId() == null) {
                    throw new SeedConsistencyException("Minigame step " + step.getId() + " is missing minigame_id");
                }
                if (questMinigameRepository.findById(step.getMinigameId()).isEmpty()) {
                    throw new SeedConsistencyException("Minigame step " + step.getId()
                            + " references missing minigame: " + step.getMinigameId());
                }
            }
            if (!isDerivedUnlockKey(unlockKey) && !hasBackendUnlockKey(quest.getLocationId(), unlockKey)) {
                throw new SeedConsistencyException("Quest step " + step.getId()
                        + " references missing backend unlock_key: " + unlockKey);
            }
        }
    }

    private void validateMinigameConfigs() {
        for (QuestMinigame game : questMinigameRepository.findAll()) {
            if (!VALID_MINIGAME_ENGINES.contains(game.getEngine())) {
                throw new SeedConsistencyException("Minigame " + game.getId()
                        + " has unsupported engine: " + game.getEngine());
            }
            try {
                JsonNode config = objectMapper.readTree(game.getConfig());
                if (!config.isObject() || !config.path("levels").isArray() || config.path("levels").isEmpty()) {
                    throw new SeedConsistencyException("Minigame " + game.getId() + " has invalid levels config");
                }
            } catch (SeedConsistencyException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new SeedConsistencyException("Minigame " + game.getId() + " has invalid JSON config");
            }
        }
    }

    private boolean hasBackendUnlockKey(UUID locationId, String unlockKey) {
        return locationId != null
                && (discoveryPointRepository.findByUnlockKeyAndLocationId(unlockKey, locationId).isPresent()
                        || artifactRepository.findByLocationIdAndUnlockKey(locationId, unlockKey).isPresent());
    }

    private static boolean isDerivedUnlockKey(String unlockKey) {
        return unlockKey.startsWith("era:") || unlockKey.startsWith("checkin:");
    }

    private void validateQuestSteps() {
        for (Quest quest : questRepository.findAll()) {
            if (quest.getStepDiscoveryKeys() == null || quest.getStepDiscoveryKeys().isBlank()) {
                continue;
            }
            UUID locationId = quest.getLocationId();
            for (String key : QuestStepProgressServiceImpl.parseSteps(quest.getStepDiscoveryKeys())) {
                if (locationId == null
                        || discoveryPointRepository
                                .findByUnlockKeyAndLocationId(key, locationId)
                                .isEmpty()) {
                    throw new SeedConsistencyException(
                            "Quest step key missing in discovery_points: " + key + " at location " + locationId);
                }
            }
        }
    }

    private void validateArtifactRequirements() {
        for (ArtifactUnlockRequirement req : artifactUnlockRequirementRepository.findAll()) {
            UUID locationId = req.getLocationId();
            String required = req.getRequiredDiscoveryKey();
            if (required == null || required.isBlank()) {
                throw new SeedConsistencyException("Empty required_discovery_key for " + req.getArtifactUnlockKey());
            }
            if (required.startsWith("artifact:")) {
                if (locationId == null
                        || artifactRepository
                                .findByLocationIdAndUnlockKey(locationId, required)
                                .isEmpty()) {
                    throw new SeedConsistencyException(
                            "Artifact requirement references missing artifact: " + required);
                }
            } else if (locationId == null
                    || discoveryPointRepository
                            .findByUnlockKeyAndLocationId(required, locationId)
                            .isEmpty()) {
                throw new SeedConsistencyException(
                        "Artifact requirement references missing discovery key: " + required);
            }
        }
    }

    private void validateUnlockRules() {
        for (UnlockRule rule : unlockRuleRepository.findAll()) {
            if (!rule.isEnabled()) {
                continue;
            }
            UUID locationId = rule.getLocationId();
            String rewardKey = rule.getRewardKey();
            if (rewardKey == null || rewardKey.isBlank()) {
                throw new SeedConsistencyException("Unlock rule missing reward_key at location " + locationId);
            }
            if ("discovery".equals(rule.getRewardType())) {
                if (locationId == null
                        || discoveryPointRepository
                                .findByUnlockKeyAndLocationId(rewardKey, locationId)
                                .isEmpty()) {
                    throw new SeedConsistencyException(
                            "Unlock rule discovery reward missing: " + rewardKey);
                }
            } else if ("artifact".equals(rule.getRewardType())) {
                if (locationId == null
                        || artifactRepository
                                .findByLocationIdAndUnlockKey(locationId, rewardKey)
                                .isEmpty()) {
                    throw new SeedConsistencyException("Unlock rule artifact reward missing: " + rewardKey);
                }
            }
        }
    }

    private void validateDiscoveryArtifactLinks() {
        for (DiscoveryArtifactLink link : discoveryArtifactLinkRepository.findAll()) {
            UUID locationId = link.getLocationId();
            String discoveryKey = link.getDiscoveryUnlockKey();
            if (locationId != null
                    && discoveryPointRepository
                            .findByUnlockKeyAndLocationId(discoveryKey, locationId)
                            .isEmpty()) {
                throw new SeedConsistencyException(
                        "discovery_artifact_links references missing discovery key: " + discoveryKey);
            }
        }
    }

    public void detectArtifactCycles() {
        Map<String, List<String>> graph = new HashMap<>();
        for (ArtifactUnlockRequirement req : artifactUnlockRequirementRepository.findAll()) {
            String required = req.getRequiredDiscoveryKey();
            if (required != null && required.startsWith("artifact:")) {
                graph.computeIfAbsent(req.getArtifactUnlockKey(), k -> new ArrayList<>()).add(required);
            }
        }
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (String node : graph.keySet()) {
            dfsArtifactCycle(graph, node, visiting, visited, new ArrayList<>());
        }
    }

    private void dfsArtifactCycle(
            Map<String, List<String>> graph,
            String node,
            Set<String> visiting,
            Set<String> visited,
            List<String> path) {
        if (visited.contains(node)) {
            return;
        }
        if (visiting.contains(node)) {
            int cycleStart = path.indexOf(node);
            List<String> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
            cycle.add(node);
            throw new SeedConsistencyException("artifact cycle detected: " + String.join(" -> ", cycle));
        }
        visiting.add(node);
        path.add(node);
        for (String next : graph.getOrDefault(node, List.of())) {
            dfsArtifactCycle(graph, next, visiting, visited, path);
        }
        path.remove(path.size() - 1);
        visiting.remove(node);
        visited.add(node);
    }
}

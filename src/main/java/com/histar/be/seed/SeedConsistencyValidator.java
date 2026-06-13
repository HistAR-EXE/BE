package com.histar.be.seed;

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
import com.histar.be.quest.repository.QuestRepository;
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

    private final QuestRepository questRepository;
    private final DiscoveryPointRepository discoveryPointRepository;
    private final ArtifactRepository artifactRepository;
    private final ArtifactUnlockRequirementRepository artifactUnlockRequirementRepository;
    private final UnlockRuleRepository unlockRuleRepository;
    private final DiscoveryArtifactLinkRepository discoveryArtifactLinkRepository;

    public void validateAll() {
        validateQuestSteps();
        validateArtifactRequirements();
        validateUnlockRules();
        validateDiscoveryArtifactLinks();
        detectArtifactCycles();
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

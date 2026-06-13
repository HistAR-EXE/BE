package com.histar.be.gamification.rules;

import com.histar.be.artifact.service.ArtifactService;
import com.histar.be.discovery.bridge.DiscoveryArtifactBridge;
import com.histar.be.discovery.service.DiscoveryService;
import com.histar.be.gamification.rules.entity.UnlockRule;
import com.histar.be.gamification.rules.repository.UnlockRuleRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnlockRuleEvaluator {

    private final UnlockRuleRepository unlockRuleRepository;
    private final DiscoveryService discoveryService;
    private final ArtifactService artifactService;
    private final DiscoveryArtifactBridge discoveryArtifactBridge;

    @Transactional
    public boolean evaluateOnCheckin(UUID userId, UUID locationId) {
        List<UnlockRule> rules =
                unlockRuleRepository.findByLocationIdAndTriggerTypeAndEnabledTrueOrderBySortOrderAsc(
                        locationId, "checkin");
        if (rules.isEmpty()) {
            return false;
        }
        for (UnlockRule rule : rules) {
            if (!matchesCheckinTrigger(rule)) {
                continue;
            }
            applyReward(userId, locationId, rule);
        }
        return true;
    }

    private boolean matchesCheckinTrigger(UnlockRule rule) {
        String triggerKey = rule.getTriggerKey();
        return triggerKey == null || triggerKey.isBlank() || "location".equals(triggerKey);
    }

    private void applyReward(UUID userId, UUID locationId, UnlockRule rule) {
        String rewardKey = resolveRewardKey(rule.getRewardKey(), locationId);
        switch (rule.getRewardType()) {
            case "discovery" -> {
                if (discoveryService.record(userId, rewardKey, locationId)) {
                    discoveryArtifactBridge.unlockLinkedArtifacts(userId, rewardKey);
                } else {
                    discoveryArtifactBridge.unlockLinkedArtifacts(userId, rewardKey);
                }
            }
            case "artifact" -> artifactService.unlockByKey(userId, rewardKey);
            default -> {
                /* unknown reward type — skip */
            }
        }
    }

    private String resolveRewardKey(String template, UUID locationId) {
        if (template != null && template.contains("{locationId}")) {
            return template.replace("{locationId}", locationId.toString());
        }
        return template;
    }
}

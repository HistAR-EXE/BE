package com.histar.be.artifact.evaluator;

import com.histar.be.artifact.service.ArtifactService;
import com.histar.be.discovery.bridge.entity.ArtifactUnlockRequirement;
import com.histar.be.discovery.bridge.entity.DiscoveryArtifactLink;
import com.histar.be.discovery.bridge.repository.ArtifactUnlockRequirementRepository;
import com.histar.be.discovery.bridge.repository.DiscoveryArtifactLinkRepository;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.artifact.repository.UserArtifactRepository;
import com.histar.be.gamification.dto.UnlockedArtifactDto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArtifactUnlockEvaluator {

    public enum UnlockSource {
        DISCOVERY,
        CHECKIN,
        QR,
        QUEST
    }

    private final DiscoveryArtifactLinkRepository linkRepository;
    private final ArtifactUnlockRequirementRepository requirementRepository;
    private final UserDiscoveryRepository userDiscoveryRepository;
    private final UserArtifactRepository userArtifactRepository;
    private final ArtifactService artifactService;

    @Transactional
    public List<UnlockedArtifactDto> evaluateFromDiscovery(UUID userId, String discoveryUnlockKey) {
        Map<UUID, UnlockedArtifactDto> collected = new LinkedHashMap<>();
        collectUnlock(userId, discoveryUnlockKey, collected);
        List<DiscoveryArtifactLink> links = linkRepository.findByDiscoveryUnlockKey(discoveryUnlockKey);
        for (DiscoveryArtifactLink link : links) {
            if (!link.getArtifactUnlockKey().equals(discoveryUnlockKey)) {
                collectUnlock(userId, link.getArtifactUnlockKey(), collected);
            }
        }
        return List.copyOf(collected.values());
    }

    @Transactional
    public void evaluateFromCheckin(UUID userId, String artifactUnlockKey) {
        evaluate(userId, artifactUnlockKey, UnlockSource.CHECKIN);
    }

    @Transactional
    public List<UnlockedArtifactDto> evaluateFromCheckinCollecting(UUID userId, String artifactUnlockKey) {
        if (!compositeRequirementsMet(userId, artifactUnlockKey)) {
            return List.of();
        }
        return artifactService.unlockByKeyCollecting(userId, artifactUnlockKey);
    }

    private void collectUnlock(UUID userId, String artifactUnlockKey, Map<UUID, UnlockedArtifactDto> collected) {
        if (!compositeRequirementsMet(userId, artifactUnlockKey)) {
            return;
        }
        for (UnlockedArtifactDto dto : artifactService.unlockByKeyCollecting(userId, artifactUnlockKey)) {
            collected.putIfAbsent(dto.id(), dto);
        }
    }

    private void evaluate(UUID userId, String artifactUnlockKey, UnlockSource source) {
        if (!compositeRequirementsMet(userId, artifactUnlockKey)) {
            return;
        }
        artifactService.unlockByKey(userId, artifactUnlockKey);
    }

    private boolean compositeRequirementsMet(UUID userId, String artifactUnlockKey) {
        List<ArtifactUnlockRequirement> requirements =
                requirementRepository.findByArtifactUnlockKey(artifactUnlockKey);
        if (requirements.isEmpty()) {
            return true;
        }
        UUID locationId = requirements.get(0).getLocationId();
        for (ArtifactUnlockRequirement req : requirements) {
            if (!requirementMet(userId, locationId, req.getRequiredDiscoveryKey())) {
                return false;
            }
        }
        return true;
    }

    private boolean requirementMet(UUID userId, UUID locationId, String requiredKey) {
        if (locationId == null) {
            return false;
        }
        if (requiredKey != null && requiredKey.startsWith("artifact:")) {
            return userDiscoveryRepository.existsByUserIdAndLocationIdAndDiscoveryKey(
                            userId, locationId, requiredKey)
                    || userArtifactRepository.existsByUserIdAndUnlockKeyAtLocation(
                            userId, locationId, requiredKey);
        }
        return userDiscoveryRepository.existsByUserIdAndLocationIdAndDiscoveryKey(
                userId, locationId, requiredKey);
    }
}

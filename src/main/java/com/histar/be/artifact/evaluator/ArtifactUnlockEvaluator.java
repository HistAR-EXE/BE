package com.histar.be.artifact.evaluator;

import com.histar.be.artifact.repository.UserArtifactRepository;
import com.histar.be.artifact.service.ArtifactService;
import com.histar.be.discovery.bridge.entity.ArtifactUnlockRequirement;
import com.histar.be.discovery.bridge.entity.DiscoveryArtifactLink;
import com.histar.be.discovery.bridge.repository.ArtifactUnlockRequirementRepository;
import com.histar.be.discovery.bridge.repository.DiscoveryArtifactLinkRepository;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import java.util.List;
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
    public void evaluateFromDiscovery(UUID userId, String discoveryUnlockKey) {
        evaluate(userId, discoveryUnlockKey, UnlockSource.DISCOVERY);
        List<DiscoveryArtifactLink> links = linkRepository.findByDiscoveryUnlockKey(discoveryUnlockKey);
        for (DiscoveryArtifactLink link : links) {
            if (!link.getArtifactUnlockKey().equals(discoveryUnlockKey)) {
                evaluate(userId, link.getArtifactUnlockKey(), UnlockSource.DISCOVERY);
            }
        }
    }

    @Transactional
    public void evaluateFromCheckin(UUID userId, String artifactUnlockKey) {
        evaluate(userId, artifactUnlockKey, UnlockSource.CHECKIN);
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

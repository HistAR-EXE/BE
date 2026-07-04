package com.histar.be.discovery.bridge;

import com.histar.be.artifact.evaluator.ArtifactUnlockEvaluator;
import com.histar.be.gamification.dto.UnlockedArtifactDto;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiscoveryArtifactBridge {

    private final ArtifactUnlockEvaluator artifactUnlockEvaluator;

    @Transactional
    public List<UnlockedArtifactDto> unlockLinkedArtifacts(UUID userId, String discoveryUnlockKey) {
        return artifactUnlockEvaluator.evaluateFromDiscovery(userId, discoveryUnlockKey);
    }
}

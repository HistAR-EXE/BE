package com.histar.be.discovery.bridge;

import com.histar.be.artifact.evaluator.ArtifactUnlockEvaluator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiscoveryArtifactBridge {

    private final ArtifactUnlockEvaluator artifactUnlockEvaluator;

    @Transactional
    public void unlockLinkedArtifacts(UUID userId, String discoveryUnlockKey) {
        artifactUnlockEvaluator.evaluateFromDiscovery(userId, discoveryUnlockKey);
    }
}

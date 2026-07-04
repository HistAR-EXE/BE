package com.histar.be.artifact.service.impl;

import com.histar.be.artifact.dto.ArtifactResponse;
import com.histar.be.artifact.dto.MyArtifactsResponse;
import com.histar.be.artifact.entity.Artifact;
import com.histar.be.artifact.entity.UserArtifact;
import com.histar.be.artifact.repository.ArtifactRepository;
import com.histar.be.artifact.repository.UserArtifactRepository;
import com.histar.be.artifact.service.ArtifactService;
import com.histar.be.gamification.dto.UnlockedArtifactDto;
import com.histar.be.profile.service.ProfileAccessPolicy;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArtifactServiceImpl implements ArtifactService {

    private static final List<String> CHECKIN_UNLOCK_KEYS = List.of(
            "artifact:cuoc-chim",
            "artifact:chong-tre",
            "artifact:nap-ham",
            "artifact:den-dau",
            "artifact:khan-ran");

    private final ArtifactRepository artifactRepository;
    private final UserArtifactRepository userArtifactRepository;
    private final ProfileAccessPolicy profileAccessPolicy;

    @Override
    @Transactional(readOnly = true)
    public List<ArtifactResponse> findCatalog(UUID locationId) {
        return artifactRepository.findByLocationIdOrderBySortOrder(locationId).stream()
                .map(a -> ArtifactResponse.from(a, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MyArtifactsResponse findMine(UUID userId, UUID locationId) {
        List<Artifact> catalog = artifactRepository.findByLocationIdOrderBySortOrder(locationId);
        boolean previewAll = profileAccessPolicy.previewsAllGamificationContent(userId);
        Set<UUID> unlockedIds = new HashSet<>();
        if (!previewAll) {
            userArtifactRepository.findByUserId(userId).forEach(ua -> unlockedIds.add(ua.getArtifactId()));
        }
        List<ArtifactResponse> items = catalog.stream()
                .map(a -> ArtifactResponse.from(a, previewAll || unlockedIds.contains(a.getId())))
                .toList();
        int collected = (int) items.stream().filter(ArtifactResponse::unlocked).count();
        return new MyArtifactsResponse(items, collected, items.size());
    }

    @Override
    @Transactional
    public boolean unlockByKey(UUID userId, String unlockKey) {
        return !unlockByKeyCollecting(userId, unlockKey).isEmpty();
    }

    @Override
    @Transactional
    public List<UnlockedArtifactDto> unlockByKeyCollecting(UUID userId, String unlockKey) {
        List<Artifact> matches = artifactRepository.findByUnlockKey(unlockKey);
        if (matches.isEmpty()) {
            return List.of();
        }
        List<UnlockedArtifactDto> unlocked = new java.util.ArrayList<>();
        for (Artifact artifact : matches) {
            if (!userArtifactRepository.existsByUserIdAndArtifactId(userId, artifact.getId())) {
                userArtifactRepository.save(UserArtifact.builder()
                        .userId(userId)
                        .artifactId(artifact.getId())
                        .unlockedAt(Instant.now())
                        .build());
                unlocked.add(new UnlockedArtifactDto(
                        artifact.getId(), artifact.getName(), artifact.getImageUrl(), artifact.getUnlockKey()));
            }
        }
        return unlocked;
    }

    @Override
    @Transactional
    public void unlockOnCheckin(UUID userId, UUID locationId) {
        for (String key : CHECKIN_UNLOCK_KEYS) {
            artifactRepository
                    .findByLocationIdAndUnlockKey(locationId, key)
                    .ifPresent(artifact -> unlockIfAbsent(userId, artifact.getId()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return artifactRepository.count();
    }

    private void unlockIfAbsent(UUID userId, UUID artifactId) {
        if (!userArtifactRepository.existsByUserIdAndArtifactId(userId, artifactId)) {
            userArtifactRepository.save(UserArtifact.builder()
                    .userId(userId)
                    .artifactId(artifactId)
                    .unlockedAt(Instant.now())
                    .build());
        }
    }
}

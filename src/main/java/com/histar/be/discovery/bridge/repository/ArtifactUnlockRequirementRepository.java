package com.histar.be.discovery.bridge.repository;

import com.histar.be.discovery.bridge.entity.ArtifactUnlockRequirement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtifactUnlockRequirementRepository extends JpaRepository<ArtifactUnlockRequirement, UUID> {

    List<ArtifactUnlockRequirement> findByArtifactUnlockKey(String artifactUnlockKey);
}

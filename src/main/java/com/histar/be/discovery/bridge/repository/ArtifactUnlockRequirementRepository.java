package com.histar.be.discovery.bridge.repository;

import com.histar.be.discovery.bridge.entity.ArtifactUnlockRequirement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtifactUnlockRequirementRepository extends JpaRepository<ArtifactUnlockRequirement, UUID> {

    List<ArtifactUnlockRequirement> findByArtifactUnlockKey(String artifactUnlockKey);

    long countByLocationIdAndRequiredDiscoveryKey(UUID locationId, String requiredDiscoveryKey);

    long countByLocationIdAndArtifactUnlockKey(UUID locationId, String artifactUnlockKey);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ArtifactUnlockRequirement r WHERE r.locationId = :locationId AND r.requiredDiscoveryKey = :requiredDiscoveryKey")
    int deleteByLocationIdAndRequiredDiscoveryKey(
            @Param("locationId") UUID locationId, @Param("requiredDiscoveryKey") String requiredDiscoveryKey);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ArtifactUnlockRequirement r WHERE r.locationId = :locationId AND r.artifactUnlockKey = :artifactUnlockKey")
    int deleteByLocationIdAndArtifactUnlockKey(
            @Param("locationId") UUID locationId, @Param("artifactUnlockKey") String artifactUnlockKey);
}

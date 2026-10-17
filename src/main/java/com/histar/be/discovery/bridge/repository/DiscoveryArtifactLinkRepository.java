package com.histar.be.discovery.bridge.repository;

import com.histar.be.discovery.bridge.entity.DiscoveryArtifactLink;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiscoveryArtifactLinkRepository extends JpaRepository<DiscoveryArtifactLink, DiscoveryArtifactLink.DiscoveryArtifactLinkId> {

    List<DiscoveryArtifactLink> findByDiscoveryUnlockKey(String discoveryUnlockKey);

    long countByLocationIdAndDiscoveryUnlockKey(UUID locationId, String discoveryUnlockKey);

    long countByLocationIdAndArtifactUnlockKey(UUID locationId, String artifactUnlockKey);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM DiscoveryArtifactLink l WHERE l.locationId = :locationId AND l.discoveryUnlockKey = :discoveryUnlockKey")
    int deleteByLocationIdAndDiscoveryUnlockKey(
            @Param("locationId") UUID locationId, @Param("discoveryUnlockKey") String discoveryUnlockKey);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM DiscoveryArtifactLink l WHERE l.locationId = :locationId AND l.artifactUnlockKey = :artifactUnlockKey")
    int deleteByLocationIdAndArtifactUnlockKey(
            @Param("locationId") UUID locationId, @Param("artifactUnlockKey") String artifactUnlockKey);
}

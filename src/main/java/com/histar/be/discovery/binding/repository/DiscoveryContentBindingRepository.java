package com.histar.be.discovery.binding.repository;

import com.histar.be.discovery.binding.entity.DiscoveryContentBinding;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiscoveryContentBindingRepository extends JpaRepository<DiscoveryContentBinding, UUID> {

    List<DiscoveryContentBinding> findByLocationIdOrderBySortOrder(UUID locationId);

    long countByLocationIdAndUnlockKey(UUID locationId, String unlockKey);

    long countByArtifactId(UUID artifactId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM DiscoveryContentBinding b WHERE b.locationId = :locationId AND b.unlockKey = :unlockKey")
    int deleteByLocationIdAndUnlockKey(
            @Param("locationId") UUID locationId, @Param("unlockKey") String unlockKey);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM DiscoveryContentBinding b WHERE b.artifactId = :artifactId")
    int deleteByArtifactId(@Param("artifactId") UUID artifactId);
}

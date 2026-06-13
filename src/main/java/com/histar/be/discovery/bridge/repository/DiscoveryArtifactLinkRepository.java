package com.histar.be.discovery.bridge.repository;

import com.histar.be.discovery.bridge.entity.DiscoveryArtifactLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscoveryArtifactLinkRepository extends JpaRepository<DiscoveryArtifactLink, DiscoveryArtifactLink.DiscoveryArtifactLinkId> {

    List<DiscoveryArtifactLink> findByDiscoveryUnlockKey(String discoveryUnlockKey);
}

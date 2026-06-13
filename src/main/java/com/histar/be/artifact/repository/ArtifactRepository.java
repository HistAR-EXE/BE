package com.histar.be.artifact.repository;

import com.histar.be.artifact.entity.Artifact;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtifactRepository extends JpaRepository<Artifact, UUID> {

    List<Artifact> findByLocationIdOrderBySortOrder(UUID locationId);

    Optional<Artifact> findByLocationIdAndUnlockKey(UUID locationId, String unlockKey);

    List<Artifact> findByUnlockKey(String unlockKey);
}

package com.histar.be.artifact.repository;

import com.histar.be.artifact.entity.Artifact;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtifactRepository extends JpaRepository<Artifact, UUID> {

    @Query("select a from Artifact a where a.locationId = :locationId and a.deletedAt is null order by a.sortOrder")
    List<Artifact> findByLocationIdOrderBySortOrder(@Param("locationId") UUID locationId);

    Optional<Artifact> findByLocationIdAndUnlockKeyAndDeletedAtIsNull(UUID locationId, String unlockKey);

    List<Artifact> findByUnlockKeyAndDeletedAtIsNull(String unlockKey);

    List<Artifact> findByUnlockMinigameIdAndRarityAndDeletedAtIsNull(UUID unlockMinigameId, String rarity);

    default Optional<Artifact> findByLocationIdAndUnlockKey(UUID locationId, String unlockKey) {
        return findByLocationIdAndUnlockKeyAndDeletedAtIsNull(locationId, unlockKey);
    }

    default List<Artifact> findByUnlockKey(String unlockKey) {
        return findByUnlockKeyAndDeletedAtIsNull(unlockKey);
    }

    default List<Artifact> findByUnlockMinigameIdAndRarity(UUID unlockMinigameId, String rarity) {
        return findByUnlockMinigameIdAndRarityAndDeletedAtIsNull(unlockMinigameId, rarity);
    }

    @Override
    @Query("select a from Artifact a where a.id = :id and a.deletedAt is null")
    Optional<Artifact> findById(@Param("id") UUID id);

    @Query("select a from Artifact a where a.id = :id")
    Optional<Artifact> findIncludingDeletedById(@Param("id") UUID id);

    List<Artifact> findByDeletedAtIsNull();

    List<Artifact> findByDeletedAtIsNotNull();

    long countByDeletedAtIsNotNull();

    long countByDeletedAtIsNull();
}

package com.histar.be.discovery.repository;

import com.histar.be.discovery.entity.DiscoveryPoint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiscoveryPointRepository extends JpaRepository<DiscoveryPoint, UUID> {

    @Query("select d from DiscoveryPoint d where d.locationId = :locationId and d.deletedAt is null order by d.sortOrder")
    List<DiscoveryPoint> findByLocationIdOrderBySortOrder(@Param("locationId") UUID locationId);

    boolean existsByUnlockKeyAndDeletedAtIsNull(String unlockKey);

    Optional<DiscoveryPoint> findFirstByUnlockKeyAndDeletedAtIsNull(String unlockKey);

    Optional<DiscoveryPoint> findByUnlockKeyAndLocationIdAndDeletedAtIsNull(String unlockKey, UUID locationId);

    default boolean existsByUnlockKey(String unlockKey) {
        return existsByUnlockKeyAndDeletedAtIsNull(unlockKey);
    }

    default Optional<DiscoveryPoint> findFirstByUnlockKey(String unlockKey) {
        return findFirstByUnlockKeyAndDeletedAtIsNull(unlockKey);
    }

    default Optional<DiscoveryPoint> findByUnlockKeyAndLocationId(String unlockKey, UUID locationId) {
        return findByUnlockKeyAndLocationIdAndDeletedAtIsNull(unlockKey, locationId);
    }

    @Override
    @Query("select d from DiscoveryPoint d where d.id = :id and d.deletedAt is null")
    Optional<DiscoveryPoint> findById(@Param("id") UUID id);

    @Query("select d from DiscoveryPoint d where d.id = :id")
    Optional<DiscoveryPoint> findIncludingDeletedById(@Param("id") UUID id);

    List<DiscoveryPoint> findByDeletedAtIsNull();

    List<DiscoveryPoint> findByDeletedAtIsNotNull();

    long countByDeletedAtIsNotNull();

    long countByDeletedAtIsNull();
}

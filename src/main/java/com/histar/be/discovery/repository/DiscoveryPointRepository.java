package com.histar.be.discovery.repository;

import com.histar.be.discovery.entity.DiscoveryPoint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscoveryPointRepository extends JpaRepository<DiscoveryPoint, UUID> {

    List<DiscoveryPoint> findByLocationIdOrderBySortOrder(UUID locationId);

    boolean existsByUnlockKey(String unlockKey);

    Optional<DiscoveryPoint> findFirstByUnlockKey(String unlockKey);

    Optional<DiscoveryPoint> findByUnlockKeyAndLocationId(String unlockKey, UUID locationId);
}

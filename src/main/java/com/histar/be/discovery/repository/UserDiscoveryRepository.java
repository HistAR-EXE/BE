package com.histar.be.discovery.repository;

import com.histar.be.discovery.entity.UserDiscovery;
import com.histar.be.discovery.entity.UserDiscoveryId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDiscoveryRepository extends JpaRepository<UserDiscovery, UserDiscoveryId> {

    List<UserDiscovery> findByUserId(UUID userId);

    List<UserDiscovery> findByUserIdAndLocationId(UUID userId, UUID locationId);

    long countByUserIdAndLocationId(UUID userId, UUID locationId);

    boolean existsByUserIdAndDiscoveryKey(UUID userId, String discoveryKey);

    boolean existsByUserIdAndLocationIdAndDiscoveryKey(UUID userId, UUID locationId, String discoveryKey);
}

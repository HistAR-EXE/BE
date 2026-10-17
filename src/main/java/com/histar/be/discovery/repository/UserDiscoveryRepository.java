package com.histar.be.discovery.repository;

import com.histar.be.discovery.entity.UserDiscovery;
import com.histar.be.discovery.entity.UserDiscoveryId;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDiscoveryRepository extends JpaRepository<UserDiscovery, UserDiscoveryId> {

    long countByUserIdInAndDiscoveredAtGreaterThanEqual(Collection<UUID> userIds, Instant from);

    @Query("select distinct ud.locationId from UserDiscovery ud where ud.userId = :userId")
    List<UUID> findDistinctLocationIdsByUserId(@Param("userId") UUID userId);

    List<UserDiscovery> findByUserId(UUID userId);

    List<UserDiscovery> findByUserIdAndLocationId(UUID userId, UUID locationId);

    List<UserDiscovery> findByUserIdAndLocationIdAndDiscoveredAtBetween(
            UUID userId, UUID locationId, Instant from, Instant to);

    long countByUserIdAndLocationId(UUID userId, UUID locationId);

    boolean existsByUserIdAndDiscoveryKey(UUID userId, String discoveryKey);

    boolean existsByUserIdAndLocationIdAndDiscoveryKey(UUID userId, UUID locationId, String discoveryKey);

    long countByLocationIdAndDiscoveryKey(UUID locationId, String discoveryKey);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM UserDiscovery ud WHERE ud.locationId = :locationId AND ud.discoveryKey = :discoveryKey")
    int deleteByLocationIdAndDiscoveryKey(
            @Param("locationId") UUID locationId, @Param("discoveryKey") String discoveryKey);
}

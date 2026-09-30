package com.histar.be.checkin.repository;

import com.histar.be.checkin.entity.Checkin;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckinRepository extends JpaRepository<Checkin, UUID> {

    long countByCreatedAtGreaterThanEqual(Instant createdAt);

    long countByUserId(UUID userId);

    boolean existsByUserIdAndLocationId(UUID userId, UUID locationId);

    List<Checkin> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Live board: check-ins of a set of users since {@code since}. */
    List<Checkin> findByUserIdInAndCreatedAtGreaterThanEqual(Collection<UUID> userIds, Instant since);

    Optional<Checkin> findByUserIdAndClientUuid(UUID userId, UUID clientUuid);

    /** Last check-in where presence was proven by a verified station QR. */
    Optional<Checkin> findFirstByUserIdAndPresenceMethodAndStationCodeIsNotNullOrderByCreatedAtDesc(
            UUID userId, String presenceMethod);

    /** Last QR check-in at a specific location (multi-site sequence). */
    Optional<Checkin> findFirstByUserIdAndLocationIdAndPresenceMethodAndStationCodeIsNotNullOrderByCreatedAtDesc(
            UUID userId, UUID locationId, String presenceMethod);

    /** Station codes the user has checked in at (used for sequential story unlock). */
    @Query("select distinct c.stationCode from Checkin c where c.userId = :userId and c.stationCode is not null")
    List<String> findDistinctStationCodesByUserId(@Param("userId") UUID userId);

    @Query(
            """
            select distinct c.stationCode from Checkin c
            where c.userId = :userId and c.locationId = :locationId and c.stationCode is not null
            """)
    List<String> findDistinctStationCodesByUserIdAndLocationId(
            @Param("userId") UUID userId, @Param("locationId") UUID locationId);

    boolean existsByUserIdAndLocationIdAndStationCodeIgnoreCase(UUID userId, UUID locationId, String stationCode);
}

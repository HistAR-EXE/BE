package com.histar.be.profile.repository;

import com.histar.be.profile.entity.Profile;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    @Query(
            """
            SELECT p FROM Profile p
            WHERE (:q = '' OR LOWER(p.email) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(p.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:role = '' OR UPPER(p.role) = :role)
              AND (:tier = '' OR UPPER(COALESCE(p.tier, '')) = :tier)
              AND (:verified IS NULL OR p.emailVerified = :verified)
              AND (:status = '' OR UPPER(COALESCE(p.accountStatus, 'ACTIVE')) = :status)
            """)
    Page<Profile> findAdminUsers(
            @Param("q") String q,
            @Param("role") String role,
            @Param("tier") String tier,
            @Param("verified") Boolean verified,
            @Param("status") String status,
            Pageable pageable);

    @Query(
            """
            SELECT p FROM Profile p
            WHERE (:q = '' OR LOWER(p.email) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(p.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:role = '' OR UPPER(p.role) = :role)
              AND (:tier = '' OR UPPER(COALESCE(p.tier, '')) = :tier)
              AND (:verified IS NULL OR p.emailVerified = :verified)
              AND (:status = '' OR UPPER(COALESCE(p.accountStatus, 'ACTIVE')) = :status)
            ORDER BY p.createdAt DESC
            """)
    List<Profile> findAdminUsersForExport(
            @Param("q") String q,
            @Param("role") String role,
            @Param("tier") String tier,
            @Param("verified") Boolean verified,
            @Param("status") String status,
            Pageable pageable);

    List<Profile> findByIdIn(List<UUID> ids);

    @Query(
            """
            SELECT p.id FROM Profile p
            WHERE LOWER(COALESCE(p.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    List<UUID> findIdsMatchingAdminSearch(@Param("q") String q);

    Optional<Profile> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Profile p WHERE p.id = :userId")
    Optional<Profile> findByIdForUpdate(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Profile p WHERE UPPER(p.role) = 'ADMIN' ORDER BY p.id")
    List<Profile> findAllAdminsForUpdate();

    List<Profile> findTop50ByOrderByTotalPointsDesc();

    List<Profile> findTop50ByCityOrderByTotalPointsDesc(String city);

    @Query(
            """
            SELECT DISTINCT p FROM Profile p
            WHERE p.totalPoints IS NOT NULL
            AND (:city IS NULL OR p.city = :city)
            ORDER BY p.totalPoints DESC
            """)
    List<Profile> findLeaderboard(
            @Param("city") String city, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT p FROM Profile p WHERE p.orgId = :orgId AND p.totalPoints IS NOT NULL ORDER BY p.totalPoints DESC")
    List<Profile> findLeaderboardByOrgId(
            @Param("orgId") UUID orgId, org.springframework.data.domain.Pageable pageable);

    @Query(
            """
            SELECT DISTINCT p FROM Profile p
            WHERE p.totalPoints IS NOT NULL
            AND (:city IS NULL OR p.city = :city)
            AND (
              EXISTS (SELECT 1 FROM Checkin c WHERE c.userId = p.id AND c.createdAt >= :weekStart)
              OR EXISTS (SELECT 1 FROM UserCreation uc WHERE uc.userId = p.id AND uc.createdAt >= :weekStart)
            )
            ORDER BY p.totalPoints DESC
            """)
    List<Profile> findLeaderboardSince(
            @Param("city") String city,
            @Param("weekStart") Instant weekStart,
            org.springframework.data.domain.Pageable pageable);
}

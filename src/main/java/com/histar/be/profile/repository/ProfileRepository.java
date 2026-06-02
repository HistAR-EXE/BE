package com.histar.be.profile.repository;

import com.histar.be.profile.entity.Profile;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByEmail(String email);

    List<Profile> findTop50ByOrderByTotalPointsDesc();

    List<Profile> findTop50ByCityOrderByTotalPointsDesc(String city);

    @Query(
            """
            SELECT DISTINCT p FROM Profile p
            WHERE p.totalPoints IS NOT NULL
            AND (:city IS NULL OR p.city = :city)
            AND (
              :weekStart IS NULL
              OR EXISTS (SELECT 1 FROM Checkin c WHERE c.userId = p.id AND c.createdAt >= :weekStart)
              OR EXISTS (SELECT 1 FROM UserCreation uc WHERE uc.userId = p.id AND uc.createdAt >= :weekStart)
            )
            ORDER BY p.totalPoints DESC
            """)
    List<Profile> findLeaderboard(
            @Param("city") String city, @Param("weekStart") Instant weekStart, org.springframework.data.domain.Pageable pageable);
}

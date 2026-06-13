package com.histar.be.visit.repository;

import com.histar.be.visit.entity.VisitSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitSessionRepository extends JpaRepository<VisitSession, UUID> {

    Optional<VisitSession> findFirstByUserIdAndLocationIdAndEndedAtIsNullOrderByStartedAtDesc(
            UUID userId, UUID locationId);

    Optional<VisitSession> findFirstByUserIdAndLocationIdAndStatusAndEndedAtIsNullOrderByStartedAtDesc(
            UUID userId, UUID locationId, String status);

    List<VisitSession> findByUserIdAndLocationIdAndStatusAndEndedAtIsNull(
            UUID userId, UUID locationId, String status);

    List<VisitSession> findByUserIdAndLocationIdOrderByStartedAtDesc(UUID userId, UUID locationId);

    List<VisitSession> findByLocationIdOrderByStartedAtDesc(UUID locationId);

    List<VisitSession> findByLocationIdAndStatusAndEndedAtIsNotNull(UUID locationId, String status);

    @Query("""
            SELECT s FROM VisitSession s
            WHERE s.status = :status AND s.endedAt IS NULL
              AND s.lastActivityAt < :cutoff
            """)
    List<VisitSession> findIdleActiveSessions(@Param("status") String status, @Param("cutoff") Instant cutoff);
}

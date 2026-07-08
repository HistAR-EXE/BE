package com.histar.be.billing.repository;

import com.histar.be.billing.entity.OrgActiveSession;
import com.histar.be.billing.entity.OrgActiveSession.OrgActiveSessionId;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrgActiveSessionRepository extends JpaRepository<OrgActiveSession, OrgActiveSessionId> {

    @Query("SELECT COUNT(s) FROM OrgActiveSession s WHERE s.orgId = :orgId AND s.lastSeenAt >= :since")
    int countActiveByOrgId(@Param("orgId") UUID orgId, @Param("since") Instant since);

    @Modifying
    @Query("DELETE FROM OrgActiveSession s WHERE s.lastSeenAt < :cutoff")
    int deleteStale(@Param("cutoff") Instant cutoff);
}

package com.histar.be.referral.repository;

import com.histar.be.referral.entity.ReferralVisit;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReferralVisitRepository extends JpaRepository<ReferralVisit, UUID> {

    boolean existsByReferralCodeAndSessionId(String referralCode, String sessionId);

    long countByReferralCode(String referralCode);

    /** Distinct sessions and signed-in users per code: {code, visits, uniqueSessions, uniqueUsers, lastVisit}. */
    @Query("""
            select v.referralCode, count(v), count(distinct v.sessionId), count(distinct v.userId), max(v.visitedAt)
            from ReferralVisit v
            where v.visitedAt >= :since
            group by v.referralCode
            """)
    List<Object[]> aggregateSince(@Param("since") Instant since);
}

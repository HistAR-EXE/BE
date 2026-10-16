package com.histar.be.discovery.explore;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExploreAttemptRepository extends JpaRepository<ExploreAttempt, UUID> {
    long countByUserIdAndSubmittedAtAfter(UUID userId, Instant since);
}

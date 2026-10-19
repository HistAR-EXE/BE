package com.histar.be.leaderboard.passportmail;

import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassportMailRepository extends JpaRepository<PassportMailAttempt,UUID> {
    Optional<PassportMailAttempt> findByUserIdAndRequestId(UUID userId,UUID requestId);
    List<PassportMailAttempt> findByUserIdAndCreatedAtGreaterThanOrderByCreatedAtAsc(UUID userId,Instant since);
}

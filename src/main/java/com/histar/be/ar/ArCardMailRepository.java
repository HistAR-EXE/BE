package com.histar.be.ar;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ArCardMailRepository extends JpaRepository<ArCardMailAttempt,UUID> {
    Optional<ArCardMailAttempt> findByUserIdAndRequestId(UUID userId,UUID requestId);
    List<ArCardMailAttempt> findByUserIdAndCreatedAtGreaterThanOrderByCreatedAtAsc(UUID userId,Instant since);
}

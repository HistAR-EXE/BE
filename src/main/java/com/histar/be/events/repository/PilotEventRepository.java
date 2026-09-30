package com.histar.be.events.repository;

import com.histar.be.events.entity.PilotEvent;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PilotEventRepository extends JpaRepository<PilotEvent, UUID> {

    boolean existsByClientUuid(UUID clientUuid);

    long countByEventTypeAndOccurredAtGreaterThanEqual(String eventType, Instant occurredAt);

    List<PilotEvent> findByEventTypeAndOccurredAtGreaterThanEqualOrderByOccurredAtDesc(
            String eventType, Instant occurredAt, Pageable pageable);

    /** Live board: pilot events of the given users of one type since {@code since}. */
    List<PilotEvent> findByUserIdInAndEventTypeAndOccurredAtGreaterThanEqual(
            Collection<UUID> userIds, String eventType, Instant since);

    /** Live board: most recent rally ("gather") event for an org (org id is embedded in payload). */
    Optional<PilotEvent> findFirstByEventTypeAndPayloadJsonContainingOrderByOccurredAtDesc(
            String eventType, String payloadFragment);
}

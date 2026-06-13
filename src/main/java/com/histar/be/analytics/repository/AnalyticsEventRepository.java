package com.histar.be.analytics.repository;

import com.histar.be.analytics.entity.AnalyticsEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {

    boolean existsByUserIdAndEventTypeAndEventKey(UUID userId, String eventType, String eventKey);

    List<AnalyticsEvent> findByUserIdAndEventTypeOrderByCreatedAtAsc(UUID userId, String eventType);

    Optional<AnalyticsEvent> findFirstByUserIdAndEventTypeAndEventKey(
            UUID userId, String eventType, String eventKey);
}

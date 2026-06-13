package com.histar.be.visit.repository;

import com.histar.be.visit.entity.VisitSessionEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitSessionEventRepository extends JpaRepository<VisitSessionEvent, UUID> {

    boolean existsByVisitSessionIdAndEventTypeAndEventKey(UUID visitSessionId, String eventType, String eventKey);

    List<VisitSessionEvent> findByVisitSessionIdOrderByCreatedAtAsc(UUID visitSessionId);
}

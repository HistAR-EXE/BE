package com.histar.be.admin.audit;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditEventRepository extends JpaRepository<AdminAuditEvent, UUID> {
    Optional<AdminAuditEvent> findByEntityTypeAndEntityIdAndActionAndIdempotencyKey(
            String entityType, UUID entityId, String action, String idempotencyKey);
}

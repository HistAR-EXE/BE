package com.histar.be.admin.audit;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdminAuditEventRepository
        extends JpaRepository<AdminAuditEvent, UUID>, JpaSpecificationExecutor<AdminAuditEvent> {
    Optional<AdminAuditEvent> findByEntityTypeAndEntityIdAndActionAndIdempotencyKey(
            String entityType, UUID entityId, String action, String idempotencyKey);
}

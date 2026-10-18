package com.histar.be.admin.audit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AdminAuditEventRepository
        extends JpaRepository<AdminAuditEvent, UUID>, JpaSpecificationExecutor<AdminAuditEvent> {
    Optional<AdminAuditEvent> findByEntityTypeAndEntityIdAndActionAndIdempotencyKey(
            String entityType, UUID entityId, String action, String idempotencyKey);

    @Query("SELECT e FROM AdminAuditEvent e ORDER BY e.occurredAt DESC, e.id DESC")
    List<AdminAuditEvent> findLatest(Pageable pageable);

    @Query("SELECT e FROM AdminAuditEvent e ORDER BY e.occurredAt ASC, e.id ASC")
    List<AdminAuditEvent> findAllOrdered(Pageable pageable);

    long countByEventHashIsNull();
}

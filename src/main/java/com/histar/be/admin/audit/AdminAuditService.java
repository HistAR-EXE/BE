package com.histar.be.admin.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.common.exception.BusinessRuleException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuditService {
    private final AdminAuditEventRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AdminAuditEvent append(AdminAuditCommand command) {
        if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
            var existing = repository.findByEntityTypeAndEntityIdAndActionAndIdempotencyKey(
                    command.entityType(), command.entityId(), command.action(), command.idempotencyKey());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        return repository.save(AdminAuditEvent.builder()
                .id(UUID.randomUUID())
                .actorId(command.actorId())
                .entityType(command.entityType())
                .entityId(command.entityId())
                .action(command.action())
                .oldRowVersion(command.oldRowVersion())
                .newRowVersion(command.newRowVersion())
                .reason(command.reason())
                .idempotencyKey(command.idempotencyKey())
                .metadataJson(write(command.metadata()))
                .occurredAt(Instant.now())
                .build());
    }

    private String write(Map<String, ?> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("Không thể lưu metadata audit");
        }
    }
}

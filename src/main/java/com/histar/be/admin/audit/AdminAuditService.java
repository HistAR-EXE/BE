package com.histar.be.admin.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.response.PageResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

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

    @Transactional(readOnly = true)
    public PageResponse<AdminAuditEventResponse> list(
            int page,
            int size,
            String entityType,
            String action,
            UUID actorId,
            UUID entityId,
            Instant from,
            Instant to,
            String sort) {
        Specification<AdminAuditEvent> filters = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType.trim().toUpperCase()));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action.trim().toUpperCase()));
            }
            if (actorId != null) predicates.add(cb.equal(root.get("actorId"), actorId));
            if (entityId != null) predicates.add(cb.equal(root.get("entityId"), entityId));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String normalizedSort = sort == null ? "" : sort.trim();
        Sort.Direction direction = normalizedSort.equalsIgnoreCase("asc")
                || normalizedSort.toLowerCase(java.util.Locale.ROOT).endsWith(",asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Page<AdminAuditEvent> result = repository.findAll(
                filters, PageRequest.of(safePage, safeSize, Sort.by(direction, "occurredAt")));
        return PageResponse.<AdminAuditEventResponse>builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalItems(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    private String write(Map<String, ?> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("Không thể lưu metadata audit");
        }
    }

    private AdminAuditEventResponse toResponse(AdminAuditEvent event) {
        try {
            Map<String, Object> metadata = objectMapper.readValue(
                    event.getMetadataJson(), new TypeReference<Map<String, Object>>() {});
            return new AdminAuditEventResponse(
                    event.getId(), event.getActorId(), event.getEntityType(), event.getEntityId(), event.getAction(),
                    event.getOldRowVersion(), event.getNewRowVersion(), event.getReason(), metadata, event.getOccurredAt());
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("Không thể đọc metadata audit");
        }
    }
}

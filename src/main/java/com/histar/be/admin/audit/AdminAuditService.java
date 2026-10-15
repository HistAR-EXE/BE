package com.histar.be.admin.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.response.PageResponse;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuditService {
    private final AdminAuditEventRepository repository;
    private final ProfileRepository profiles;
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
                .metadataJson(write(enrichMetadata(command.metadata())))
                .occurredAt(Instant.now())
                .build());
    }

    /** Merges client IP / coarse location into audit metadata without overwriting caller keys. */
    Map<String, Object> enrichMetadata(Map<String, ?> metadata) {
        Map<String, Object> enriched = new HashMap<>();
        if (metadata != null) {
            enriched.putAll(metadata);
        }
        ClientRequestInfo.Snapshot request = ClientRequestInfo.current();
        if (request.clientIp() != null
                && !enriched.containsKey("clientIp")
                && !enriched.containsKey("ip")) {
            enriched.put("clientIp", request.clientIp());
        }
        if (request.location() != null
                && !enriched.containsKey("location")
                && !enriched.containsKey("city")) {
            enriched.put("location", request.location());
        }
        return enriched;
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
        return list(page, size, entityType, action, null, actorId, entityId, from, to, sort);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminAuditEventResponse> list(
            int page,
            int size,
            String entityType,
            String action,
            String actor,
            UUID actorId,
            UUID entityId,
            Instant from,
            Instant to,
            String sort) {
        String actorQuery = actor == null ? null : actor.trim();
        List<UUID> actorIdsFromSearch = null;
        if (actorQuery != null && !actorQuery.isEmpty()) {
            actorIdsFromSearch = profiles.findIdsMatchingAdminSearch(actorQuery);
            if (actorIdsFromSearch.isEmpty()) {
                int safePage = Math.max(page, 0);
                int safeSize = Math.min(Math.max(size, 1), 100);
                return PageResponse.<AdminAuditEventResponse>builder()
                        .items(List.of())
                        .page(safePage)
                        .size(safeSize)
                        .totalItems(0)
                        .totalPages(0)
                        .build();
            }
        }

        List<UUID> resolvedActorIds = actorIdsFromSearch;
        Specification<AdminAuditEvent> filters = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType.trim().toUpperCase(Locale.ROOT)));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action.trim().toUpperCase(Locale.ROOT)));
            }
            if (resolvedActorIds != null) {
                predicates.add(root.get("actorId").in(resolvedActorIds));
            }
            if (actorId != null) {
                predicates.add(cb.equal(root.get("actorId"), actorId));
            }
            if (entityId != null) {
                predicates.add(cb.equal(root.get("entityId"), entityId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String normalizedSort = sort == null ? "" : sort.trim();
        Sort.Direction direction = normalizedSort.equalsIgnoreCase("asc")
                || normalizedSort.toLowerCase(Locale.ROOT).endsWith(",asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Page<AdminAuditEvent> result = repository.findAll(
                filters, PageRequest.of(safePage, safeSize, Sort.by(direction, "occurredAt")));

        List<UUID> pageActorIds = result.getContent().stream()
                .map(AdminAuditEvent::getActorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, Profile> actorsById = pageActorIds.isEmpty()
                ? Map.of()
                : profiles.findAllById(pageActorIds).stream()
                        .collect(Collectors.toMap(Profile::getId, Function.identity()));

        return PageResponse.<AdminAuditEventResponse>builder()
                .items(result.getContent().stream().map(event -> toResponse(event, actorsById)).toList())
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

    private AdminAuditEventResponse toResponse(AdminAuditEvent event, Map<UUID, Profile> actorsById) {
        try {
            Map<String, Object> metadata = objectMapper.readValue(
                    event.getMetadataJson(), new TypeReference<Map<String, Object>>() {});
            Profile actor = event.getActorId() == null ? null : actorsById.get(event.getActorId());
            return new AdminAuditEventResponse(
                    event.getId(),
                    event.getActorId(),
                    actor == null ? null : actor.getDisplayName(),
                    actor == null ? null : maskEmail(actor.getEmail()),
                    event.getEntityType(),
                    event.getEntityId(),
                    event.getAction(),
                    event.getOldRowVersion(),
                    event.getNewRowVersion(),
                    event.getReason(),
                    metadata,
                    event.getOccurredAt());
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("Không thể đọc metadata audit");
        }
    }

    static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return "***";
        }
        return email.charAt(0) + "***@" + email.substring(at + 1);
    }
}

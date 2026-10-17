package com.histar.be.admin.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.response.PageResponse;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.ArrayList;
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

    private static final int INTEGRITY_CAP = 50_000;
    private static final int EXPORT_CAP = 10_000;

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
        backfillHashChainIfNeeded();
        String prevHash = tipEventHash();
        UUID id = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        String metadataJson = write(enrichMetadata(command.metadata()));
        String eventHash = AdminAuditHashChain.computeEventHash(
                id,
                occurredAt,
                command.actorId(),
                command.entityType(),
                command.entityId(),
                command.action(),
                command.oldRowVersion(),
                command.newRowVersion(),
                command.reason(),
                command.idempotencyKey(),
                metadataJson,
                prevHash);
        return repository.save(AdminAuditEvent.builder()
                .id(id)
                .actorId(command.actorId())
                .entityType(command.entityType())
                .entityId(command.entityId())
                .action(command.action())
                .oldRowVersion(command.oldRowVersion())
                .newRowVersion(command.newRowVersion())
                .reason(command.reason())
                .idempotencyKey(command.idempotencyKey())
                .metadataJson(metadataJson)
                .occurredAt(occurredAt)
                .prevEventHash(prevHash)
                .eventHash(eventHash)
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
        ActorFilter actorFilter = resolveActorFilter(actor, actorId);
        if (actorFilter.emptySearch()) {
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

        Specification<AdminAuditEvent> filters =
                buildFilters(entityType, action, actorFilter, entityId, from, to);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String normalizedSort = sort == null ? "" : sort.trim();
        Sort.Direction direction = normalizedSort.equalsIgnoreCase("asc")
                        || normalizedSort.toLowerCase(Locale.ROOT).endsWith(",asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Page<AdminAuditEvent> result = repository.findAll(
                filters, PageRequest.of(safePage, safeSize, Sort.by(direction, "occurredAt").and(Sort.by(direction, "id"))));

        return PageResponse.<AdminAuditEventResponse>builder()
                .items(toResponses(result.getContent()))
                .page(result.getNumber())
                .size(result.getSize())
                .totalItems(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional
    public AdminAuditIntegrityResponse verifyIntegrity(
            String entityType,
            String action,
            String actor,
            UUID actorId,
            UUID entityId,
            Instant from,
            Instant to) {
        backfillHashChainIfNeeded();
        ActorFilter actorFilter = resolveActorFilter(actor, actorId);
        if (actorFilter.emptySearch()) {
            return new AdminAuditIntegrityResponse(true, 0, null, null, null, from, to);
        }
        Specification<AdminAuditEvent> filters =
                buildFilters(entityType, action, actorFilter, entityId, from, to);
        long total = repository.count(filters);
        if (total > INTEGRITY_CAP) {
            throw new BusinessRuleException(
                    "Quá nhiều bản ghi để kiểm tra integrity (tối đa " + INTEGRITY_CAP + "). Thu hẹp bộ lọc.");
        }
        List<AdminAuditEvent> events = repository.findAll(
                filters, PageRequest.of(0, INTEGRITY_CAP, Sort.by(Sort.Direction.ASC, "occurredAt").and(Sort.by(Sort.Direction.ASC, "id"))))
                .getContent();

        String lastExpected = null;
        String lastStored = null;
        for (AdminAuditEvent event : events) {
            String prev = event.getPrevEventHash() == null || event.getPrevEventHash().isBlank()
                    ? AdminAuditHashChain.GENESIS_PREV_HASH
                    : event.getPrevEventHash();
            String expected = AdminAuditHashChain.computeEventHash(event, prev);
            String stored = event.getEventHash();
            if (stored == null || !stored.equalsIgnoreCase(expected)) {
                return new AdminAuditIntegrityResponse(
                        false, events.size(), lastStored, expected, event.getId(), from, to);
            }
            lastExpected = expected;
            lastStored = stored;
        }
        return new AdminAuditIntegrityResponse(
                true, events.size(), lastStored, lastExpected, null, from, to);
    }

    @Transactional
    public AdminAuditExportEnvelope exportJson(
            String entityType,
            String action,
            String actor,
            UUID actorId,
            UUID entityId,
            Instant from,
            Instant to) {
        backfillHashChainIfNeeded();
        ActorFilter actorFilter = resolveActorFilter(actor, actorId);
        if (actorFilter.emptySearch()) {
            return new AdminAuditExportEnvelope(Instant.now(), 0, 0, false, List.of());
        }
        Specification<AdminAuditEvent> filters =
                buildFilters(entityType, action, actorFilter, entityId, from, to);
        long total = repository.count(filters);
        boolean truncated = total > EXPORT_CAP;
        List<AdminAuditEvent> events = repository.findAll(
                        filters,
                        PageRequest.of(
                                0,
                                EXPORT_CAP,
                                Sort.by(Sort.Direction.ASC, "occurredAt").and(Sort.by(Sort.Direction.ASC, "id"))))
                .getContent();
        return new AdminAuditExportEnvelope(
                Instant.now(), total, events.size(), truncated, toResponses(events));
    }

    @Transactional
    public int backfillHashChainIfNeeded() {
        if (repository.count() == 0 || repository.countByEventHashIsNull() == 0) {
            return 0;
        }
        List<AdminAuditEvent> all = new ArrayList<>();
        int page = 0;
        while (true) {
            List<AdminAuditEvent> batch = repository.findAllOrdered(PageRequest.of(page, 1000));
            if (batch.isEmpty()) {
                break;
            }
            all.addAll(batch);
            if (batch.size() < 1000) {
                break;
            }
            page++;
        }
        String prev = AdminAuditHashChain.GENESIS_PREV_HASH;
        for (AdminAuditEvent event : all) {
            String hash = AdminAuditHashChain.computeEventHash(event, prev);
            event.setPrevEventHash(prev);
            event.setEventHash(hash);
            prev = hash;
        }
        repository.saveAll(all);
        return all.size();
    }

    private String tipEventHash() {
        List<AdminAuditEvent> tip = repository.findLatest(PageRequest.of(0, 1));
        if (tip.isEmpty()) {
            return AdminAuditHashChain.GENESIS_PREV_HASH;
        }
        String hash = tip.get(0).getEventHash();
        if (hash == null || hash.isBlank()) {
            return AdminAuditHashChain.GENESIS_PREV_HASH;
        }
        return hash;
    }

    private ActorFilter resolveActorFilter(String actor, UUID actorId) {
        String actorQuery = actor == null ? null : actor.trim();
        if (actorQuery == null || actorQuery.isEmpty()) {
            return new ActorFilter(null, actorId, false);
        }
        List<UUID> actorIdsFromSearch = profiles.findIdsMatchingAdminSearch(actorQuery);
        if (actorIdsFromSearch.isEmpty()) {
            return new ActorFilter(List.of(), actorId, true);
        }
        return new ActorFilter(actorIdsFromSearch, actorId, false);
    }

    private Specification<AdminAuditEvent> buildFilters(
            String entityType,
            String action,
            ActorFilter actorFilter,
            UUID entityId,
            Instant from,
            Instant to) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType.trim().toUpperCase(Locale.ROOT)));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action.trim().toUpperCase(Locale.ROOT)));
            }
            if (actorFilter.actorIdsFromSearch() != null) {
                predicates.add(root.get("actorId").in(actorFilter.actorIdsFromSearch()));
            }
            if (actorFilter.actorId() != null) {
                predicates.add(cb.equal(root.get("actorId"), actorFilter.actorId()));
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
    }

    private List<AdminAuditEventResponse> toResponses(List<AdminAuditEvent> events) {
        List<UUID> pageActorIds = events.stream()
                .map(AdminAuditEvent::getActorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, Profile> actorsById = pageActorIds.isEmpty()
                ? Map.of()
                : profiles.findAllById(pageActorIds).stream()
                        .collect(Collectors.toMap(Profile::getId, Function.identity()));
        return events.stream().map(event -> toResponse(event, actorsById)).toList();
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
                    event.getOccurredAt(),
                    event.getEventHash(),
                    event.getPrevEventHash());
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

    private record ActorFilter(List<UUID> actorIdsFromSearch, UUID actorId, boolean emptySearch) {}
}

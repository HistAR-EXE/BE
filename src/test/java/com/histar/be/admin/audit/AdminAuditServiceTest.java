package com.histar.be.admin.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AdminAuditServiceTest {

    @Mock private AdminAuditEventRepository repository;
    @Mock private ProfileRepository profiles;
    @Captor private ArgumentCaptor<AdminAuditEvent> eventCaptor;

    private AdminAuditService service;

    @BeforeEach
    void setUp() {
        service = new AdminAuditService(repository, profiles, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    void appendsImmutableAuditEventWithHashChain() {
        UUID actorId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        when(repository.findByEntityTypeAndEntityIdAndActionAndIdempotencyKey(
                        "QUEST", questId, "SOFT_DELETED", "delete-quest-1"))
                .thenReturn(Optional.empty());
        when(repository.count()).thenReturn(0L);
        when(repository.findLatest(any(Pageable.class))).thenReturn(List.of());
        when(repository.save(any(AdminAuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.append(new AdminAuditCommand(
                actorId,
                "QUEST",
                questId,
                "SOFT_DELETED",
                4L,
                5L,
                "Sai nguồn",
                "delete-quest-1",
                Map.of("title", "Củ Chi")));

        verify(repository).save(eventCaptor.capture());
        AdminAuditEvent event = eventCaptor.getValue();
        assertThat(event.getActorId()).isEqualTo(actorId);
        assertThat(event.getEntityType()).isEqualTo("QUEST");
        assertThat(event.getEntityId()).isEqualTo(questId);
        assertThat(event.getAction()).isEqualTo("SOFT_DELETED");
        assertThat(event.getPrevEventHash()).isEqualTo(AdminAuditHashChain.GENESIS_PREV_HASH);
        assertThat(event.getEventHash()).isEqualTo(AdminAuditHashChain.computeEventHash(event, event.getPrevEventHash()));
        assertThat(event.getEventHash()).hasSize(64);
    }

    @Test
    void hashChainIsDeterministic() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Instant at = Instant.parse("2026-10-15T00:00:00Z");
        String first = AdminAuditHashChain.computeEventHash(
                id, at, null, "USER", id, "ROLE_CHANGED", null, null, "reason", null, "{}", AdminAuditHashChain.GENESIS_PREV_HASH);
        String second = AdminAuditHashChain.computeEventHash(
                id, at, null, "USER", id, "ROLE_CHANGED", null, null, "reason", null, "{}", AdminAuditHashChain.GENESIS_PREV_HASH);
        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }

    @Test
    void verifyIntegrityDetectsTamperedEventHash() {
        UUID id = UUID.randomUUID();
        Instant at = Instant.parse("2026-10-15T12:00:00Z");
        String prev = AdminAuditHashChain.GENESIS_PREV_HASH;
        String goodHash = AdminAuditHashChain.computeEventHash(
                id, at, null, "USER", id, "ROLE_CHANGED", 1L, 2L, "ok", null, "{}", prev);
        AdminAuditEvent event = AdminAuditEvent.builder()
                .id(id)
                .entityType("USER")
                .entityId(id)
                .action("ROLE_CHANGED")
                .oldRowVersion(1L)
                .newRowVersion(2L)
                .reason("ok")
                .metadataJson("{}")
                .occurredAt(at)
                .prevEventHash(prev)
                .eventHash("deadbeef" + "0".repeat(56))
                .build();
        when(repository.count()).thenReturn(1L);
        when(repository.countByEventHashIsNull()).thenReturn(0L);
        when(repository.count(any(Specification.class))).thenReturn(1L);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 50_000), 1));

        var result = service.verifyIntegrity(null, null, null, null, null, null, null);

        assertThat(result.valid()).isFalse();
        assertThat(result.brokenEventId()).isEqualTo(id);
        assertThat(result.expectedTip()).isEqualTo(goodHash);
        assertThat(result.checkedCount()).isEqualTo(1);
    }

    @Test
    void verifyIntegrityAcceptsValidChain() {
        UUID id = UUID.randomUUID();
        Instant at = Instant.parse("2026-10-15T12:00:00Z");
        String prev = AdminAuditHashChain.GENESIS_PREV_HASH;
        String hash = AdminAuditHashChain.computeEventHash(
                id, at, null, "USER", id, "ROLE_CHANGED", null, null, null, null, "{}", prev);
        AdminAuditEvent event = AdminAuditEvent.builder()
                .id(id)
                .entityType("USER")
                .entityId(id)
                .action("ROLE_CHANGED")
                .metadataJson("{}")
                .occurredAt(at)
                .prevEventHash(prev)
                .eventHash(hash)
                .build();
        when(repository.count()).thenReturn(1L);
        when(repository.countByEventHashIsNull()).thenReturn(0L);
        when(repository.count(any(Specification.class))).thenReturn(1L);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 50_000), 1));

        var result = service.verifyIntegrity(null, null, null, null, null, null, null);

        assertThat(result.valid()).isTrue();
        assertThat(result.brokenEventId()).isNull();
        assertThat(result.chainTip()).isEqualTo(hash);
        assertThat(result.expectedTip()).isEqualTo(hash);
    }

    @Test
    void enrichMetadataKeepsCallerKeysAndAddsLocalhostSnapshot() {
        Map<String, Object> enriched = service.enrichMetadata(Map.of("title", "Củ Chi", "clientIp", "8.8.8.8"));
        assertThat(enriched.get("title")).isEqualTo("Củ Chi");
        assertThat(enriched.get("clientIp")).isEqualTo("8.8.8.8");
    }

    @Test
    void searchesActorsAndReturnsDisplayNameWithMaskedEmail() {
        UUID actorId = UUID.randomUUID();
        AdminAuditEvent event = AdminAuditEvent.builder()
                .id(UUID.randomUUID())
                .actorId(actorId)
                .entityType("USER")
                .entityId(UUID.randomUUID())
                .action("ROLE_CHANGED")
                .metadataJson("{}")
                .occurredAt(Instant.parse("2026-10-15T00:00:00Z"))
                .eventHash("a".repeat(64))
                .prevEventHash(AdminAuditHashChain.GENESIS_PREV_HASH)
                .build();
        Profile actor = Profile.builder()
                .id(actorId)
                .displayName("Alice Admin")
                .email("alice@example.com")
                .build();
        when(profiles.findIdsMatchingAdminSearch("alice")).thenReturn(List.of(actorId));
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1));
        when(profiles.findAllById(List.of(actorId))).thenReturn(List.of(actor));

        var response = service.list(0, 20, null, null, " alice ", actorId, null, null, null, "desc");

        assertThat(response.getItems()).singleElement().satisfies(item -> {
            assertThat(item.actorId()).isEqualTo(actorId);
            assertThat(item.actorDisplayName()).isEqualTo("Alice Admin");
            assertThat(item.actorEmail()).isEqualTo("a***@example.com");
            assertThat(item.eventHash()).isEqualTo("a".repeat(64));
        });
        verify(profiles).findIdsMatchingAdminSearch("alice");
        verify(profiles).findAllById(List.of(actorId));
    }

    @Test
    void retainsExactActorIdFilterWithoutRunningActorTextSearch() {
        UUID actorId = UUID.randomUUID();
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.list(0, 20, null, null, null, actorId, null, null, null, "desc");

        verify(profiles, never()).findIdsMatchingAdminSearch(any());
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }
}

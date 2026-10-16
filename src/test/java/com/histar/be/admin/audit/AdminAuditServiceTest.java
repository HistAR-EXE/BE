package com.histar.be.admin.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void appendsImmutableAuditEventForSoftDelete() {
        UUID actorId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        when(repository.findByEntityTypeAndEntityIdAndActionAndIdempotencyKey(
                "QUEST", questId, "SOFT_DELETED", "delete-quest-1")).thenReturn(Optional.empty());
        when(repository.save(any(AdminAuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminAuditService service = new AdminAuditService(
                repository, profiles, new com.fasterxml.jackson.databind.ObjectMapper());

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
        assertThat(event.getOldRowVersion()).isEqualTo(4L);
        assertThat(event.getNewRowVersion()).isEqualTo(5L);
        assertThat(event.getReason()).isEqualTo("Sai nguồn");
        assertThat(event.getIdempotencyKey()).isEqualTo("delete-quest-1");
        assertThat(event.getOccurredAt()).isBeforeOrEqualTo(Instant.now());
        assertThat(event.getMetadataJson()).contains("title");
    }

    @Test
    void enrichMetadataKeepsCallerKeysAndAddsLocalhostSnapshot() {
        AdminAuditService service = new AdminAuditService(
                repository, profiles, new com.fasterxml.jackson.databind.ObjectMapper());
        Map<String, Object> enriched = service.enrichMetadata(Map.of("title", "Củ Chi", "clientIp", "8.8.8.8"));
        assertThat(enriched.get("title")).isEqualTo("Củ Chi");
        assertThat(enriched.get("clientIp")).isEqualTo("8.8.8.8");
    }

    @Test
    void resolveLocationLabelsLoopbackAndPrivateNetworks() {
        assertThat(ClientRequestInfo.resolveLocation("127.0.0.1")).isEqualTo("Localhost");
        assertThat(ClientRequestInfo.resolveLocation("192.168.1.10")).isEqualTo("Mạng nội bộ");
        assertThat(ClientRequestInfo.resolveLocation("172.20.0.5")).isEqualTo("Mạng nội bộ");
        assertThat(ClientRequestInfo.resolveLocation("14.232.18.91")).isNull();
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
        AdminAuditService service = new AdminAuditService(
                repository, profiles, new com.fasterxml.jackson.databind.ObjectMapper());

        var response = service.list(
                0, 20, null, null, " alice ", actorId, null, null, null, "desc");

        assertThat(response.getItems()).singleElement().satisfies(item -> {
            assertThat(item.actorId()).isEqualTo(actorId);
            assertThat(item.actorDisplayName()).isEqualTo("Alice Admin");
            assertThat(item.actorEmail()).isEqualTo("a***@example.com");
        });
        verify(profiles).findIdsMatchingAdminSearch("alice");
        verify(profiles).findAllById(List.of(actorId));
    }

    @Test
    void retainsExactActorIdFilterWithoutRunningActorTextSearch() {
        UUID actorId = UUID.randomUUID();
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        AdminAuditService service = new AdminAuditService(
                repository, profiles, new com.fasterxml.jackson.databind.ObjectMapper());

        service.list(0, 20, null, null, null, actorId, null, null, null, "desc");

        verify(profiles, never()).findIdsMatchingAdminSearch(any());
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }
}

package com.histar.be.admin.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAuditServiceTest {

    @Mock private AdminAuditEventRepository repository;
    @Captor private ArgumentCaptor<AdminAuditEvent> eventCaptor;

    @Test
    void appendsImmutableAuditEventForSoftDelete() {
        UUID actorId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        when(repository.findByEntityTypeAndEntityIdAndActionAndIdempotencyKey(
                "QUEST", questId, "SOFT_DELETED", "delete-quest-1")).thenReturn(Optional.empty());
        when(repository.save(any(AdminAuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminAuditService service = new AdminAuditService(repository, new com.fasterxml.jackson.databind.ObjectMapper());

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
    }
}

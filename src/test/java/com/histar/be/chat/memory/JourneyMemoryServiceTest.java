package com.histar.be.chat.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JourneyMemoryServiceTest {

    @Mock
    private JourneyMemoryRepository repository;

    @InjectMocks
    private JourneyMemoryService service;

    @Test
    void returns_no_memory_when_the_visitor_disabled_it() {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(repository.findByUserIdAndLocationId(userId, locationId))
                .thenReturn(Optional.of(JourneyMemory.disabled(userId, locationId)));

        assertThat(service.findActive(userId, locationId)).isEmpty();
    }

    @Test
    void persists_a_minimal_memory_with_ninety_day_expiry() {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(repository.findByUserIdAndLocationId(userId, locationId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        JourneyMemory saved = service.remember(userId, locationId, "Hầm bí mật", "quest: Đang khám phá", "vi", "LIGHT_HINT");

        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plusSeconds(89L * 24 * 60 * 60));
        assertThat(saved.isEnabled()).isTrue();
        verify(repository).save(any(JourneyMemory.class));
    }

    @Test
    void does_not_reenable_memory_that_the_visitor_disabled() {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(repository.findByUserIdAndLocationId(userId, locationId))
                .thenReturn(Optional.of(JourneyMemory.disabled(userId, locationId)));

        service.observe(userId, locationId, "Hầm bí mật", "quest: Đang khám phá", "vi", "LIGHT_HINT");

        verify(repository, never()).save(any());
    }
}

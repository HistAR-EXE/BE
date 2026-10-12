package com.histar.be.character.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.character.entity.CharacterEntity;
import com.histar.be.character.repository.CharacterRepository;
import com.histar.be.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CharacterServiceImplTest {

    @Mock private CharacterRepository repository;

    @Test
    void findsOnlyEnabledChatAgentsForLocation() {
        UUID locationId = UUID.randomUUID();
        CharacterEntity chrono = CharacterEntity.builder()
                .id(UUID.randomUUID())
                .locationId(locationId)
                .name("Chrono — Trợ lý TimeLens")
                .chatEnabled(true)
                .build();
        when(repository.findByLocationIdAndChatEnabledTrueOrderByNameAsc(locationId)).thenReturn(List.of(chrono));

        CharacterServiceImpl service = new CharacterServiceImpl(repository);

        assertThat(service.findChatAgentsByLocationId(locationId)).containsExactly(chrono);
        verify(repository).findByLocationIdAndChatEnabledTrueOrderByNameAsc(locationId);
    }

    @Test
    void rejectsDisabledCharacterAsNewChatAgent() {
        UUID characterId = UUID.randomUUID();
        when(repository.findByIdAndChatEnabledTrue(characterId)).thenReturn(Optional.empty());

        CharacterServiceImpl service = new CharacterServiceImpl(repository);

        assertThatThrownBy(() -> service.findEnabledChatAgentById(characterId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Enabled chat agent not found");
        verify(repository).findByIdAndChatEnabledTrue(characterId);
    }
}

package com.histar.be.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PersonaMapperTest {

    @Test
    void resolvesEveryLegacyNameToTheChronoPersona() {
        assertThat(PersonaMapper.resolvePersonaKey("Chị Năm")).isEqualTo("chrono");
        assertThat(PersonaMapper.resolvePersonaKey("Anh Ba")).isEqualTo("chrono");
        assertThat(PersonaMapper.resolvePersonaKey("Chrono — Trợ lý TimeLens")).isEqualTo("chrono");
    }
}

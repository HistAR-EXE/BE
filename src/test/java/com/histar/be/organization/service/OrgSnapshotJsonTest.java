package com.histar.be.organization.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.histar.be.config.JacksonConfig;
import com.histar.be.liveboard.dto.LiveBoardSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrgSnapshotJsonTest {
    @Test void configuredMapperPersistsAndReadsSnapshotTimesWithoutMockingJson() throws Exception {
        var mapper = new JacksonConfig().objectMapper();
        Instant time = Instant.parse("2026-10-27T04:30:00.123456Z");
        var snapshot = new LiveBoardSnapshot(UUID.randomUUID(), time, time.minusSeconds(86400), 0, 0, null, List.of());
        String json = mapper.writeValueAsString(snapshot);
        assertThat(json).contains("2026-10-27T04:30:00.123456Z");
        assertThat(mapper.readValue(json, LiveBoardSnapshot.class)).isEqualTo(snapshot);
    }
}

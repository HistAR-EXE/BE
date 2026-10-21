package com.histar.be.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.histar.be.common.exception.ErrorCode;
import com.histar.be.common.exception.BaseException;
import com.histar.be.stations.entity.Station;
import com.histar.be.stations.entity.StationBlockType;
import com.histar.be.stations.entity.StationContentBlock;
import com.histar.be.stations.repository.StationContentBlockRepository;
import com.histar.be.stations.repository.StationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StationChatContextResolverTest {

    @Mock StationRepository stationRepository;
    @Mock StationContentBlockRepository blockRepository;
    @InjectMocks StationChatContextResolver resolver;

    @Test
    void resolvesNormalizedActiveStationAndPublishedTextSummary() {
        UUID stationId = UUID.randomUUID();
        Station station = Station.builder().id(stationId).siteCode("cu-chi").code("ST01").name("Phòng họp").build();
        when(stationRepository.findBySiteCodeAndCodeAndActiveTrue("cu-chi", "ST01"))
                .thenReturn(Optional.of(station));
        when(blockRepository.findByStationIdOrderBySortOrderAsc(stationId)).thenReturn(List.of(
                StationContentBlock.builder().blockType(StationBlockType.TEXT).body("  Nơi diễn ra các cuộc họp. ").build(),
                StationContentBlock.builder().blockType(StationBlockType.IMAGE).body("không lấy").build()));

        var result = resolver.resolve(" CU-CHI ", "st01").orElseThrow();

        assertThat(result.siteCode()).isEqualTo("cu-chi");
        assertThat(result.stationCode()).isEqualTo("ST01");
        assertThat(result.stationName()).isEqualTo("Phòng họp");
        assertThat(result.summary()).isEqualTo("Nơi diễn ra các cuộc họp.");
    }

    @Test
    void rejectsUnknownStationContextWithDedicatedCode() {
        when(stationRepository.findBySiteCodeAndCodeAndActiveTrue("cu-chi", "ST99"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve("cu-chi", "ST99"))
                .isInstanceOf(BaseException.class)
                .extracting(error -> ((BaseException) error).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATION_CONTEXT);
    }
}

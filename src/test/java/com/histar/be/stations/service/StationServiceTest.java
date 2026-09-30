package com.histar.be.stations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.stations.dto.StationBlockRequest;
import com.histar.be.stations.dto.StationImportItem;
import com.histar.be.stations.dto.StationResponse;
import com.histar.be.stations.entity.Station;
import com.histar.be.stations.entity.StationBlockType;
import com.histar.be.stations.entity.StationContentBlock;
import com.histar.be.stations.repository.StationContentBlockRepository;
import com.histar.be.stations.repository.StationRepository;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StationServiceTest {

    @Mock
    private StationRepository stationRepository;

    @Mock
    private StationContentBlockRepository blockRepository;

    @InjectMocks
    private StationService stationService;

    @Test
    void seedJsonParsesAsSixStations() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/data/cu-chi-stations.json")) {
            assertThat(in).isNotNull();
            List<StationImportItem> items = new ObjectMapper().readValue(in, new TypeReference<>() {});
            assertThat(items).hasSize(6);
            assertThat(items).allSatisfy(i -> assertThat(i.blocks()).isNotEmpty());
        }
    }

    @Test
    void getActiveThrowsWhenMissing() {
        when(stationRepository.findBySiteCodeAndCodeAndActiveTrue("cu-chi", "ST99"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> stationService.getActive("cu-chi", "ST99"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void importRejectsEmptyAndDuplicateCodes() {
        assertThatThrownBy(() -> stationService.importStations("cu-chi", List.of()))
                .isInstanceOf(BusinessRuleException.class);
        StationImportItem a = new StationImportItem("ST01", "A", null, null, null, null, null, null);
        assertThatThrownBy(() -> stationService.importStations("cu-chi", List.of(a, a)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void importUpsertsStationAndBlocks() {
        when(stationRepository.findBySiteCodeAndCode("cu-chi", "ST01")).thenReturn(Optional.empty());
        when(stationRepository.saveAndFlush(any(Station.class))).thenAnswer(inv -> {
            Station s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(blockRepository.saveAll(any())).thenAnswer(inv -> {
            List<StationContentBlock> blocks = inv.getArgument(0);
            blocks.forEach(b -> b.setId(UUID.randomUUID()));
            return blocks;
        });

        StationImportItem item = new StationImportItem(
                "ST01",
                "Bến Dược",
                null,
                null,
                null,
                "cu-chi:ST01",
                null,
                List.of(new StationBlockRequest(StationBlockType.TEXT, "t", "body", null, null, null)));

        List<StationResponse> result = stationService.importStations("cu-chi", List.of(item));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).active()).isTrue();
        assertThat(result.get(0).sortOrder()).isEqualTo(1);
        assertThat(result.get(0).blocks()).hasSize(1);
        assertThat(result.get(0).blocks().get(0).blockType()).isEqualTo(StationBlockType.TEXT);
    }
}

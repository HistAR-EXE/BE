package com.histar.be.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.histar.be.checkin.entity.Checkin;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.stations.entity.Station;
import com.histar.be.stations.repository.StationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckinHistoryServiceTest {

    @Mock CheckinRepository checkinRepository;
    @Mock LocationRepository locationRepository;
    @Mock StationRepository stationRepository;
    @InjectMocks CheckinHistoryService service;

    @Test
    void returnsLatestSuccessfulCheckinPerStationForCurrentUserAndLocation() {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        Instant newest = Instant.parse("2026-10-21T08:30:00Z");
        Checkin latestSt01 = checkin(userId, locationId, "st01", newest, "QR");
        Checkin olderSt01 = checkin(userId, locationId, "ST01", newest.minusSeconds(60), "QR");
        Checkin st02 = checkin(userId, locationId, "ST02", newest.minusSeconds(30), "QR");
        Checkin locationOnly = checkin(userId, locationId, null, newest.plusSeconds(30), "GPS");

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(
                Location.builder().id(locationId).siteCode("cu-chi").build()));
        when(checkinRepository.findByUserIdAndLocationIdOrderByCreatedAtDesc(userId, locationId))
                .thenReturn(List.of(locationOnly, latestSt01, st02, olderSt01));
        when(stationRepository.findBySiteCodeOrderBySortOrderAsc("cu-chi")).thenReturn(List.of(
                Station.builder().code("ST01").name("Phòng họp").siteCode("cu-chi").build(),
                Station.builder().code("ST02").name("Kho ngầm").siteCode("cu-chi").build()));

        var result = service.history(userId, locationId);

        assertThat(result).extracting(item -> item.stationCode()).containsExactly("ST01", "ST02");
        assertThat(result.get(0).id()).isEqualTo(latestSt01.getId());
        assertThat(result.get(0).stationName()).isEqualTo("Phòng họp");
        assertThat(result.get(0).checkedInAt()).isEqualTo(newest);
        assertThat(result.get(0).siteCode()).isEqualTo("cu-chi");
        verify(checkinRepository).findByUserIdAndLocationIdOrderByCreatedAtDesc(userId, locationId);
        verifyNoMoreInteractions(checkinRepository);
    }

    private static Checkin checkin(
            UUID userId, UUID locationId, String stationCode, Instant createdAt, String presenceMethod) {
        return Checkin.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .locationId(locationId)
                .stationCode(stationCode)
                .createdAt(createdAt)
                .presenceMethod(presenceMethod)
                .build();
    }
}

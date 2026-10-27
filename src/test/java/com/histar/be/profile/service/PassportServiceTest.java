package com.histar.be.profile.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.histar.be.billing.service.UsageQuotaService;
import com.histar.be.checkin.entity.Checkin;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.quest.progress.entity.UserQuestProgress;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.quest.repository.QuestRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PassportServiceTest {
    private final CheckinRepository checkins = mock(CheckinRepository.class);
    private final UserQuestProgressRepository progress = mock(UserQuestProgressRepository.class);
    private final PassportService service = new PassportService(checkins, progress, mock(LocationRepository.class),
            mock(QuestRepository.class), mock(UsageQuotaService.class));
    private final UUID userId = UUID.randomUUID(), locationId = UUID.randomUUID();

    @Test void completedQuestWithMissingCompletionTimeRetainsUnknownStampDate() {
        when(progress.findByUserIdAndStatus(userId,"completed")).thenReturn(List.of(UserQuestProgress.builder()
                .locationId(locationId).startedAt(Instant.parse("2026-10-01T00:00:00Z")).status("completed").build()));
        var stamps = service.getPassport(userId).stamps();
        assertThat(stamps).hasSize(1);
        assertThat(stamps.get(0).locationId()).isEqualTo(locationId);
        assertThat(stamps.get(0).completedAt()).isNull();
    }

    @Test void checkinWithUnknownTimeDoesNotDisappearFromPassport() {
        when(checkins.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(Checkin.builder().locationId(locationId).build()));
        var stamps = service.getPassport(userId).stamps();
        assertThat(stamps).hasSize(1);
        assertThat(stamps.get(0).completedAt()).isNull();
    }

    @Test void unknownCompletionNeverReplacesKnownCheckinWithQuestStartTime() {
        Instant checkinTime = Instant.parse("2026-10-01T00:00:00Z");
        when(checkins.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(Checkin.builder()
                .locationId(locationId).createdAt(checkinTime).build()));
        when(progress.findByUserIdAndStatus(userId,"completed")).thenReturn(List.of(UserQuestProgress.builder()
                .locationId(locationId).startedAt(checkinTime.plusSeconds(3600)).status("completed").build()));
        assertThat(service.getPassport(userId).stamps().get(0).completedAt()).isEqualTo(checkinTime);
    }
}

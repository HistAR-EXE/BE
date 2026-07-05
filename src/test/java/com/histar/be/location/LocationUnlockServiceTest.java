package com.histar.be.location.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.gamification.dto.QuestCompletedDto;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.userquestprogress.entity.UserQuestProgress;
import com.histar.be.profile.service.ProfileAccessPolicy;
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocationUnlockServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private UserQuestProgressRepository userQuestProgressRepository;

    @Mock
    private ProfileAccessPolicy profileAccessPolicy;

    @InjectMocks
    private LocationUnlockService locationUnlockService;

    @Test
    void isUnlocked_returnsTrueWhenNoPrerequisite() {
        Location loc = Location.builder().id(UUID.randomUUID()).name("Open").build();
        assertThat(locationUnlockService.isUnlocked(UUID.randomUUID(), loc)).isTrue();
    }

    @Test
    void isUnlocked_returnsTrueForAdminEvenWhenPrerequisiteNotCompleted() {
        UUID userId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        Location loc = Location.builder()
                .id(UUID.randomUUID())
                .name("Locked")
                .unlockPrerequisiteQuestId(questId)
                .build();
        when(profileAccessPolicy.previewsAllGamificationContent(userId)).thenReturn(true);
        assertThat(locationUnlockService.isUnlocked(userId, loc)).isTrue();
    }

    @Test
    void isUnlocked_returnsFalseWhenPrerequisiteNotCompleted() {
        UUID userId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        Location loc = Location.builder()
                .id(UUID.randomUUID())
                .name("Locked")
                .unlockPrerequisiteQuestId(questId)
                .build();
        when(userQuestProgressRepository.findByUserIdAndQuestId(userId, questId)).thenReturn(Optional.empty());
        assertThat(locationUnlockService.isUnlocked(userId, loc)).isFalse();
    }

    @Test
    void isUnlocked_returnsTrueWhenPrerequisiteCompleted() {
        UUID userId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        Location loc = Location.builder()
                .id(UUID.randomUUID())
                .name("Locked")
                .unlockPrerequisiteQuestId(questId)
                .build();
        UserQuestProgress progress = new UserQuestProgress();
        progress.setStatus(QuestStatus.COMPLETED);
        when(userQuestProgressRepository.findByUserIdAndQuestId(userId, questId)).thenReturn(Optional.of(progress));
        assertThat(locationUnlockService.isUnlocked(userId, loc)).isTrue();
    }

    @Test
    void resolveNewlyUnlocked_returnsLocationsAfterQuestComplete() {
        UUID userId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        Location unlocked = Location.builder()
                .id(UUID.randomUUID())
                .name("Bến Nhà Rồng")
                .unlockPrerequisiteQuestId(questId)
                .unlockNarrative("Manh mối từ Củ Chi...")
                .build();
        when(locationRepository.findByUnlockPrerequisiteQuestIdIn(List.of(questId))).thenReturn(List.of(unlocked));
        UserQuestProgress progress = new UserQuestProgress();
        progress.setStatus(QuestStatus.COMPLETED);
        when(userQuestProgressRepository.findByUserIdAndQuestId(eq(userId), any())).thenReturn(Optional.of(progress));

        var result = locationUnlockService.resolveNewlyUnlocked(
                userId, List.of(new QuestCompletedDto(questId, 50, List.of())));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Bến Nhà Rồng");
        assertThat(result.get(0).isUnlocked()).isTrue();
    }
}

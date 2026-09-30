package com.histar.be.story.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.profile.service.TierAccessService;
import com.histar.be.story.dto.StoryChapterResponse;
import com.histar.be.story.entity.StoryChapter;
import com.histar.be.story.repository.StoryChapterRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoryServiceTest {

    @Mock
    private StoryChapterRepository chapterRepository;

    @Mock
    private TierAccessService tierAccessService;

    @Mock
    private CheckinRepository checkinRepository;

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private StoryService storyService;

    private final UUID cuChiLocationId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void stubLocation() {
        when(locationRepository.findBySiteCodeIgnoreCase("cu-chi"))
                .thenReturn(Optional.of(Location.builder().id(cuChiLocationId).siteCode("cu-chi").build()));
    }

    private List<StoryChapter> sixChapters() {
        List<StoryChapter> list = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            list.add(StoryChapter.builder()
                    .id(UUID.randomUUID())
                    .chapterNumber(i)
                    .stationCode("ST0" + i)
                    .title("Chương " + i)
                    .synopsis("Tóm tắt " + i)
                    .requiresPremium(i >= 3)
                    .sortOrder(i)
                    .build());
        }
        return list;
    }

    @Test
    void anonymousUnlocksOnlyFirstFreeChapter() {
        when(chapterRepository.findBySiteCodeOrderBySortOrderAscChapterNumberAsc("cu-chi"))
                .thenReturn(sixChapters());

        List<StoryChapterResponse> result = storyService.listChapters("cu-chi", null);

        assertThat(result).hasSize(6);
        assertThat(result.get(0).unlocked()).isTrue();
        assertThat(result.get(0).synopsis()).isNotNull();
        assertThat(result.subList(1, 6)).allSatisfy(c -> {
            assertThat(c.unlocked()).isFalse();
            assertThat(c.synopsis()).isNull();
        });
        verifyNoInteractions(tierAccessService, checkinRepository);
    }

    @Test
    void premiumUserWithoutProgressOnlyUnlocksFirstChapter() {
        UUID userId = UUID.randomUUID();
        when(tierAccessService.hasStoryAccess(userId, "cu-chi")).thenReturn(true);
        when(checkinRepository.findDistinctStationCodesByUserIdAndLocationId(userId, cuChiLocationId))
                .thenReturn(List.of());
        when(chapterRepository.findBySiteCodeOrderBySortOrderAscChapterNumberAsc("cu-chi"))
                .thenReturn(sixChapters());

        List<StoryChapterResponse> result = storyService.listChapters("cu-chi", userId);

        assertThat(result.get(0).unlocked()).isTrue();
        assertThat(result.subList(1, 6)).allSatisfy(c -> {
            assertThat(c.unlocked()).isFalse();
            assertThat(c.lockReason()).isEqualTo(StoryService.LOCK_SEQUENCE);
        });
    }

    @Test
    void freeUserWithSt01CompletedUnlocksChapter2NotPremium() {
        UUID userId = UUID.randomUUID();
        when(tierAccessService.hasStoryAccess(userId, "cu-chi")).thenReturn(false);
        when(checkinRepository.findDistinctStationCodesByUserIdAndLocationId(eq(userId), any()))
                .thenReturn(List.of("ST01"));
        when(chapterRepository.findBySiteCodeOrderBySortOrderAscChapterNumberAsc("cu-chi"))
                .thenReturn(sixChapters());

        List<StoryChapterResponse> result = storyService.listChapters("cu-chi", userId);

        assertThat(result.get(0).unlocked()).isTrue();
        assertThat(result.get(0).completed()).isTrue();
        assertThat(result.get(1).unlocked()).isTrue();
        assertThat(result.get(2).unlocked()).isFalse();
        assertThat(result.get(2).lockReason()).isEqualTo(StoryService.LOCK_PREMIUM);
    }
}

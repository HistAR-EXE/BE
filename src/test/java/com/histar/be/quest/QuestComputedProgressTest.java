package com.histar.be.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.discovery.service.DiscoveryService;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.service.QuestProgressService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestComputedProgressTest {

    private static final String KEY_A = "scene:22222222-2222-2222-2222-222222222221";
    private static final String KEY_B = "scene:22222222-2222-2222-2222-222222222223";

    @Autowired
    private QuestProgressService questProgressService;

    @Autowired
    private DiscoveryService discoveryService;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private DiscoveryPointRepository discoveryPointRepository;

    private UUID userId;
    private UUID questId;
    private UUID locationId;

    @BeforeEach
    void setUp() {
        Location location = locationRepository.save(Location.builder()
                .name("Củ Chi")
                .latitude(11.143)
                .longitude(106.461)
                .city("TP.HCM")
                .createdAt(Instant.now())
                .build());
        locationId = location.getId();

        for (String key : new String[] {KEY_A, KEY_B, "era:1948"}) {
            discoveryPointRepository.save(DiscoveryPoint.builder()
                    .locationId(locationId)
                    .name(key)
                    .mapXPct(BigDecimal.TEN)
                    .mapYPct(BigDecimal.TEN)
                    .unlockKey(key)
                    .sortOrder(1)
                    .build());
        }

        Quest quest = questRepository.save(Quest.builder()
                .locationId(locationId)
                .title("Multi-step quest")
                .description("Desc")
                .pointsReward(100)
                .stepDiscoveryKeys(KEY_A + "," + KEY_B)
                .requiredOrder(1)
                .build());
        questId = quest.getId();

        Profile profile = profileRepository.save(Profile.builder()
                .email("quest-computed-" + UUID.randomUUID() + "@test.local")
                .displayName("Tester")
                .role("USER")
                .totalPoints(0)
                .level(1)
                .createdAt(Instant.now())
                .build());
        userId = profile.getId();
    }

    @Test
    void case1_discoverBeforeStart_backfillsStepCount() {
        discoveryService.record(userId, KEY_A, locationId);
        discoveryService.record(userId, KEY_B, locationId);

        var progress = questProgressService.startQuest(userId, questId);

        assertEquals(QuestStatus.IN_PROGRESS, progress.status());
        assertEquals(2, progress.currentStep());
        assertEquals(3, progress.stepsTotal());
    }

    @Test
    void case2_startThenDiscoverOutOfOrder_countsSetNotSequence() {
        questProgressService.startQuest(userId, questId);
        discoveryService.record(userId, KEY_B, locationId);
        discoveryService.record(userId, KEY_A, locationId);

        var progress = questProgressService.getProgress(userId, questId);

        assertEquals(2, progress.currentStep());
        assertEquals(3, progress.stepsTotal());
    }

    @Test
    void case3_partialDiscovery_showsPartialCount() {
        questProgressService.startQuest(userId, questId);
        discoveryService.record(userId, KEY_A, locationId);

        var progress = questProgressService.getProgress(userId, questId);

        assertEquals(1, progress.currentStep());
        assertEquals(3, progress.stepsTotal());
    }
}

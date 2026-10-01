package com.histar.be.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.discovery.service.DiscoveryService;
import com.histar.be.gamification.service.GamificationService;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.service.QuestProgressService;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
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
class QuestCompletionSelfHealTest {

    @Autowired
    private GamificationService gamificationService;

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

    @Autowired
    private UserQuestProgressRepository userQuestProgressRepository;

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

        discoveryPointRepository.save(DiscoveryPoint.builder()
                .locationId(locationId)
                .name("Kitchen")
                .mapXPct(BigDecimal.TEN)
                .mapYPct(BigDecimal.TEN)
                .unlockKey("scene:22222222-2222-2222-2222-222222222221")
                .sortOrder(1)
                .build());

        Quest quest = questRepository.save(Quest.builder()
                .locationId(locationId)
                .title("Check-in first quest")
                .description("Desc")
                .pointsReward(50)
                .stepDiscoveryKeys("scene:22222222-2222-2222-2222-222222222221")
                .requiredOrder(0)
                .build());
        questId = quest.getId();

        Profile profile = profileRepository.save(Profile.builder()
                .email("self-heal-" + UUID.randomUUID() + "@test.local")
                .displayName("Tester")
                .role("USER")
                .totalPoints(0)
                .level(1)
                .createdAt(Instant.now())
                .build());
        userId = profile.getId();
    }

    @Test
    void checkinBeforeStart_thenDiscover_thenStartQuest_autoCompletes() {
        gamificationService.processCheckin(
                userId, locationId, 11.143, 106.461, "timelens:location:" + locationId);

        discoveryService.record(userId, "scene:22222222-2222-2222-2222-222222222221", locationId);
        questProgressService.startQuest(userId, questId);

        var progress = userQuestProgressRepository.findByUserIdAndQuestId(userId, questId).orElseThrow();
        assertEquals(QuestStatus.COMPLETED, progress.getStatus());
    }
}

package com.histar.be.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.histar.be.discovery.service.DiscoveryService;
import com.histar.be.gamification.service.GamificationService;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.service.CompletionTrigger;
import com.histar.be.quest.service.QuestCompletionService;
import com.histar.be.quest.service.QuestProgressService;
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
class QuestCompletionIdempotencyTest {

    @Autowired
    private QuestCompletionService questCompletionService;

    @Autowired
    private QuestProgressService questProgressService;

    @Autowired
    private GamificationService gamificationService;

    @Autowired
    private DiscoveryService discoveryService;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private ProfileRepository profileRepository;

    private UUID userId;
    private UUID locationId;

    @BeforeEach
    void setUp() {
        locationId = locationRepository
                .save(Location.builder()
                        .name("Củ Chi")
                        .latitude(11.143)
                        .longitude(106.461)
                        .city("TP.HCM")
                        .createdAt(Instant.now())
                        .build())
                .getId();

        Quest quest = questRepository.save(Quest.builder()
                .locationId(locationId)
                .title("Simple check-in quest")
                .pointsReward(80)
                .requiredOrder(0)
                .build());

        userId = profileRepository
                .save(Profile.builder()
                        .email("idempotent-" + UUID.randomUUID() + "@test.local")
                        .displayName("Tester")
                        .role("USER")
                        .totalPoints(0)
                        .level(1)
                        .createdAt(Instant.now())
                        .build())
                .getId();

        gamificationService.processCheckin(
                userId, locationId, 11.143, 106.461, "timelens:location:" + locationId);
        questProgressService.startQuest(userId, quest.getId());
    }

    @Test
    void doubleTryComplete_doesNotDoubleAwardPoints() {
        questCompletionService.tryComplete(userId, locationId, CompletionTrigger.START_QUEST);
        questCompletionService.tryComplete(userId, locationId, CompletionTrigger.DISCOVERY);

        Profile profile = profileRepository.findById(userId).orElseThrow();
        // 30 check-in nền (setUp, lần đầu) + 80 quest (trao đúng một lần, không nhân đôi)
        assertEquals(110, profile.getTotalPoints());
    }
}

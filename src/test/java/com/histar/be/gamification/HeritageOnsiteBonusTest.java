package com.histar.be.gamification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.histar.be.badge.entity.Badge;
import com.histar.be.badge.repository.BadgeRepository;
import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.discovery.service.DiscoveryService;
import com.histar.be.gamification.repository.UserHeritageOnsiteBonusRepository;
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
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
class HeritageOnsiteBonusTest {

    private static final List<String> STEP_KEYS = List.of(
            "artifact:heritage-test-rong-mai",
            "era:2026",
            "artifact:heritage-test-vali-may");

    @Autowired
    private GamificationService gamificationService;

    @Autowired
    private QuestProgressService questProgressService;

    @Autowired
    private QuestCompletionService questCompletionService;

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
    private BadgeRepository badgeRepository;

    @Autowired
    private UserQuestProgressRepository userQuestProgressRepository;

    @Autowired
    private UserHeritageOnsiteBonusRepository onsiteBonusRepository;

    private UUID userId;
    private UUID questId;
    private UUID locationId;

    @BeforeEach
    void setUp() {
        Location location = locationRepository.save(Location.builder()
                .name("Heritage test site")
                .latitude(10.77)
                .longitude(106.70)
                .city("TP.HCM")
                .createdAt(Instant.now())
                .build());
        locationId = location.getId();

        int order = 1;
        for (String key : STEP_KEYS) {
            discoveryPointRepository.save(DiscoveryPoint.builder()
                    .locationId(locationId)
                    .name(key)
                    .mapXPct(BigDecimal.TEN)
                    .mapYPct(BigDecimal.TEN)
                    .unlockKey(key)
                    .sortOrder(order++)
                    .build());
        }

        Quest quest = questRepository.save(Quest.builder()
                .locationId(locationId)
                .title("Heritage discovery quest")
                .description("Test")
                .pointsReward(80)
                .requiredOrder(1)
                .completionTrigger("discovery")
                .stepDiscoveryKeys(String.join(",", STEP_KEYS))
                .stepsTotal(3)
                .build());
        questId = quest.getId();

        badgeRepository.save(Badge.builder()
                .name("Đã đến nơi · Test site")
                .conditionType("heritage_onsite")
                .conditionValue(1)
                .locationId(locationId)
                .build());

        Profile profile = profileRepository.save(Profile.builder()
                .email("heritage-bonus@test.local")
                .passwordHash("hash")
                .displayName("Heritage tester")
                .provider("local")
                .role("USER")
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build());
        userId = profile.getId();
    }

    private void completeDiscoveryQuestOnline() {
        questProgressService.startQuest(userId, questId);
        for (String key : STEP_KEYS) {
            discoveryService.record(userId, key, locationId);
            questCompletionService.tryComplete(userId, locationId, CompletionTrigger.DISCOVERY);
        }
        var progress = userQuestProgressRepository.findByUserIdAndQuestId(userId, questId).orElseThrow();
        assertEquals(QuestStatus.COMPLETED, progress.getStatus());
    }

    @Test
    void checkinAfterOnlineComplete_awardsBonusXpAndBadgeOnce() {
        completeDiscoveryQuestOnline();

        Profile beforeCheckin = profileRepository.findById(userId).orElseThrow();
        assertEquals(80, beforeCheckin.getTotalPoints());

        var first = gamificationService.processCheckin(
                userId, locationId, 10.77, 106.70, "timelens:location:" + locationId);

        assertTrue(first.success());
        assertEquals(25, first.bonusXpAwarded());
        assertTrue(first.badgesEarned().stream().anyMatch(b -> b.name().contains("Đã đến nơi")));
        assertTrue(onsiteBonusRepository.existsByIdUserIdAndIdLocationId(userId, locationId));

        Profile afterFirst = profileRepository.findById(userId).orElseThrow();
        assertEquals(105, afterFirst.getTotalPoints());

        var second = gamificationService.processCheckin(
                userId, locationId, 10.77, 106.70, "timelens:location:" + locationId);
        assertEquals(0, second.bonusXpAwarded());

        Profile afterSecond = profileRepository.findById(userId).orElseThrow();
        assertEquals(105, afterSecond.getTotalPoints());
    }

    @Test
    void checkinBeforeOnlineComplete_doesNotAwardBonusUntilQuestDone() {
        gamificationService.processCheckin(
                userId, locationId, 10.77, 106.70, "timelens:location:" + locationId);

        completeDiscoveryQuestOnline();

        Profile profile = profileRepository.findById(userId).orElseThrow();
        assertEquals(105, profile.getTotalPoints());
        assertTrue(onsiteBonusRepository.existsByIdUserIdAndIdLocationId(userId, locationId));
    }

    @Test
    void checkinWithoutQuestComplete_doesNotAwardBonus() {
        var result = gamificationService.processCheckin(
                userId, locationId, 10.77, 106.70, "timelens:location:" + locationId);

        assertEquals(0, result.bonusXpAwarded());
        assertFalse(onsiteBonusRepository.existsByIdUserIdAndIdLocationId(userId, locationId));
        assertEquals(0, profileRepository.findById(userId).orElseThrow().getTotalPoints());
    }
}

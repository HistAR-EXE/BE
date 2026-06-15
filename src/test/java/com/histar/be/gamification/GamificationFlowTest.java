package com.histar.be.gamification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.histar.be.badge.repository.BadgeRepository;
import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import java.math.BigDecimal;
import com.histar.be.gamification.service.GamificationService;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.service.QuestProgressService;
import com.histar.be.userbadge.repository.UserBadgeRepository;
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
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
class GamificationFlowTest {

    @Autowired
    private GamificationService gamificationService;

    @Autowired
    private QuestProgressService questProgressService;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserQuestProgressRepository userQuestProgressRepository;

    @Autowired
    private UserBadgeRepository userBadgeRepository;

    @Autowired
    private BadgeRepository badgeRepository;

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

        for (String key : List.of("era:2026", "photo:cua-ham", "photo:gieng")) {
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
                .title("Test quest")
                .description("Desc")
                .story("Secret story text")
                .pointsReward(100)
                .requiredOrder(1)
                .build());
        questId = quest.getId();

        badgeRepository.save(com.histar.be.badge.entity.Badge.builder()
                .name("Explorer badge")
                .conditionType("quest_complete")
                .conditionValue(1)
                .build());

        Profile profile = profileRepository.save(Profile.builder()
                .email("gamification@test.local")
                .passwordHash("hash")
                .displayName("Tester")
                .provider("local")
                .role("USER")
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build());
        userId = profile.getId();
    }

    @Test
    void checkin_completesQuest_awardsPointsAndBadges() {
        questProgressService.startQuest(userId, questId);

        var result = gamificationService.processCheckin(
                userId, locationId, 11.143, 106.461, "timelens:location:" + locationId);

        assertTrue(result.success());
        assertEquals(1, result.questsCompleted().size());
        assertFalse(result.badgesEarned().isEmpty());

        Profile updated = profileRepository.findById(userId).orElseThrow();
        assertEquals(100, updated.getTotalPoints());
        assertEquals(2, updated.getLevel());

        var progress = userQuestProgressRepository.findByUserIdAndQuestId(userId, questId).orElseThrow();
        assertEquals(QuestStatus.COMPLETED, progress.getStatus());
    }

    @Test
    void repeatCheckin_evaluatesCheckinBadges() {
        badgeRepository.save(com.histar.be.badge.entity.Badge.builder()
                .name("Hai lan check-in")
                .conditionType("checkin")
                .conditionValue(2)
                .build());

        gamificationService.processCheckin(
                userId, locationId, 11.143, 106.461, "timelens:location:" + locationId);
        var second = gamificationService.processCheckin(
                userId, locationId, 11.143, 106.461, "timelens:location:" + locationId);

        assertTrue(second.success());
        assertTrue(second.questsCompleted().isEmpty());
        assertTrue(second.badgesEarned().stream().anyMatch(b -> "Hai lan check-in".equals(b.name())));
    }

    @Test
    void repeatCheckin_logsVisitWithoutExtraRewards() {
        questProgressService.startQuest(userId, questId);
        gamificationService.processCheckin(
                userId, locationId, 11.143, 106.461, "timelens:location:" + locationId);

        Profile afterFirst = profileRepository.findById(userId).orElseThrow();
        int pointsAfterFirst = afterFirst.getTotalPoints();

        var second = gamificationService.processCheckin(
                userId, locationId, 11.143, 106.461, "timelens:location:" + locationId);
        assertTrue(second.success());
        assertTrue(second.questsCompleted().isEmpty());
        assertEquals(0, second.bonusXpAwarded());

        Profile afterSecond = profileRepository.findById(userId).orElseThrow();
        assertEquals(pointsAfterFirst, afterSecond.getTotalPoints());
    }

    @Test
    void checkinBeforeStart_thenStartQuest_selfHealsToCompleted() {
        gamificationService.processCheckin(
                userId, locationId, 11.143, 106.461, "timelens:location:" + locationId);

        questProgressService.startQuest(userId, questId);

        var progress = userQuestProgressRepository.findByUserIdAndQuestId(userId, questId).orElseThrow();
        assertEquals(QuestStatus.COMPLETED, progress.getStatus());

        var second = gamificationService.processCheckin(
                userId, locationId, 11.143, 106.461, "timelens:location:" + locationId);
        assertTrue(second.success());
        assertTrue(second.questsCompleted().isEmpty());
    }

    @Test
    void secret_unlock_afterQuestComplete() {
        questProgressService.startQuest(userId, questId);
        gamificationService.processCheckin(
                userId, locationId, 11.143, 106.461, "timelens:location:" + locationId);

        var secretCheckin = gamificationService.processCheckin(
                userId, locationId, 11.143, 106.461, "timelens:secret:" + locationId);
        assertTrue(secretCheckin.secretUnlocked());

        var story = gamificationService.getSecretStory(userId, locationId);
        assertFalse(story.locked());
        assertEquals("Secret story text", story.story());
    }
}

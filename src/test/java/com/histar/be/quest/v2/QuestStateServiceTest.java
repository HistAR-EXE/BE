package com.histar.be.quest.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.discovery.service.DiscoveryService;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.entity.QuestStep;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.repository.QuestStepRepository;
import com.histar.be.quest.v2.dto.QuestV2Card;
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
class QuestStateServiceTest {

    private static final String SCENE_A = "scene:v2-a";
    private static final String SCENE_B = "scene:v2-b";
    private static final String XUA_NAY = "xuanay:bep-hoang-cam";
    private static final String ERA_1948 = "era:1948";

    @Autowired private QuestStateService questStateService;
    @Autowired private DiscoveryService discoveryService;
    @Autowired private LocationRepository locationRepository;
    @Autowired private QuestRepository questRepository;
    @Autowired private QuestStepRepository questStepRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private DiscoveryPointRepository discoveryPointRepository;

    private UUID locationId;
    private UUID userId;
    private UUID journeyId;
    private UUID afterId;
    private UUID keysId;

    @BeforeEach
    void setUp() {
        locationId = locationRepository.save(Location.builder()
                        .name("Củ Chi v2")
                        .latitude(11.143)
                        .longitude(106.461)
                        .city("TP.HCM")
                        .createdAt(Instant.now())
                        .build())
                .getId();
        for (String key : new String[] {SCENE_A, SCENE_B, XUA_NAY, ERA_1948}) {
            discoveryPointRepository.save(DiscoveryPoint.builder()
                    .locationId(locationId)
                    .name("Điểm " + key)
                    .mapXPct(BigDecimal.TEN)
                    .mapYPct(BigDecimal.TEN)
                    .unlockKey(key)
                    .sortOrder(1)
                    .build());
        }

        Quest journey = questRepository.save(Quest.builder()
                .locationId(locationId)
                .title("Hành trình")
                .pointsReward(100)
                .track("ONLINE")
                .sortOrder(10)
                .completionTrigger("discovery")
                .stepDiscoveryKeys(String.join(",", SCENE_A, SCENE_B, XUA_NAY))
                .stepsTotal(4)
                .build());
        journeyId = journey.getId();
        step(journey, 1, SCENE_A, "tour", true, false);
        step(journey, 2, SCENE_B, "tour", true, false);
        step(journey, 3, XUA_NAY, "xuanay", true, false);
        step(journey, 4, ERA_1948, "portal", false, true);

        Quest after = questRepository.save(Quest.builder()
                .locationId(locationId)
                .title("Sau hành trình")
                .pointsReward(50)
                .track("ONLINE")
                .sortOrder(20)
                .completionTrigger("discovery")
                .unlockAfterQuestId(journeyId)
                .stepDiscoveryKeys(SCENE_A)
                .stepsTotal(1)
                .build());
        afterId = after.getId();
        step(after, 1, SCENE_A, "tour", true, false);

        Quest byKeys = questRepository.save(Quest.builder()
                .locationId(locationId)
                .title("Mở bằng khóa")
                .pointsReward(50)
                .track("ONLINE")
                .sortOrder(30)
                .completionTrigger("discovery")
                .unlockDiscoveryKeys(SCENE_A + "," + SCENE_B)
                .stepDiscoveryKeys(XUA_NAY)
                .stepsTotal(1)
                .build());
        keysId = byKeys.getId();
        step(byKeys, 1, XUA_NAY, "xuanay", true, false);

        userId = profileRepository.save(Profile.builder()
                        .email("quest-v2-" + UUID.randomUUID() + "@test.local")
                        .displayName("Tester")
                        .role("USER")
                        .totalPoints(0)
                        .level(1)
                        .createdAt(Instant.now())
                        .build())
                .getId();
    }

    private void step(Quest quest, int order, String key, String action, boolean required, boolean premium) {
        questStepRepository.save(QuestStep.builder()
                .quest(quest)
                .stepOrder(order)
                .unlockKey(key)
                .title("Bước " + order)
                .actionType(action)
                .portalEra("portal".equals(action) ? 1948 : null)
                .required(required)
                .premium(premium)
                .build());
    }

    private QuestV2Card card(List<QuestV2Card> cards, UUID id) {
        return cards.stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void anonymousSeesLockReasonsAndSortedList() {
        List<QuestV2Card> cards = questStateService.list(locationId, "ONLINE", null);
        assertEquals(List.of(journeyId, afterId, keysId), cards.stream().map(QuestV2Card::id).toList());
        assertEquals(QuestStateService.STATUS_AVAILABLE, card(cards, journeyId).status());
        assertEquals(QuestStateService.LOCK_AFTER_QUEST, card(cards, afterId).lock().reason());
        assertEquals(QuestStateService.LOCK_DISCOVERY_KEYS, card(cards, keysId).lock().reason());
        assertEquals(3, card(cards, journeyId).progress().requiredTotal());
        assertEquals(1, card(cards, journeyId).progress().optionalTotal());
        assertTrue(questStateService.list(locationId, "ONSITE", null).isEmpty());
    }

    @Test
    void preDiscoveredStepsCountAndStartCompletesImmediatelyWithoutPremium() {
        discoveryService.record(userId, SCENE_A, locationId);
        discoveryService.record(userId, SCENE_B, locationId);
        discoveryService.record(userId, XUA_NAY, locationId);

        var available = questStateService.detail(journeyId, userId);
        assertEquals(QuestStateService.STATUS_AVAILABLE, available.quest().status());
        assertEquals(3, available.quest().progress().requiredDone());
        assertTrue(available.steps().get(0).preDiscovered());
        assertEquals("PREMIUM_LOCKED", available.steps().get(3).state());

        var started = questStateService.start(journeyId, userId);
        assertFalse(started.completed().isEmpty());
        assertEquals(QuestStateService.STATUS_COMPLETED, started.detail().quest().status());
        assertNotNull(started.detail().quest().completedAt());
    }

    @Test
    void backendEnforcesAfterQuestAndKeyLocks() {
        assertThrows(BusinessRuleException.class, () -> questStateService.start(afterId, userId));
        assertThrows(BusinessRuleException.class, () -> questStateService.start(keysId, userId));

        discoveryService.record(userId, SCENE_A, locationId);
        discoveryService.record(userId, SCENE_B, locationId);
        assertEquals(QuestStateService.STATUS_AVAILABLE,
                card(questStateService.list(locationId, null, userId), keysId).status());
    }

    @Test
    void reconcileIsIdempotent() {
        questStateService.start(journeyId, userId);
        discoveryService.record(userId, SCENE_A, locationId);
        discoveryService.record(userId, SCENE_B, locationId);
        discoveryService.record(userId, XUA_NAY, locationId);
        questStateService.reconcile(locationId, userId);
        assertTrue(questStateService.reconcile(locationId, userId).isEmpty());
        assertEquals(QuestStateService.STATUS_COMPLETED, questStateService.detail(journeyId, userId).quest().status());
    }

    @Test
    void rejectsUnknownTrack() {
        assertThrows(BusinessRuleException.class, () -> questStateService.list(locationId, "MOON", null));
    }
}

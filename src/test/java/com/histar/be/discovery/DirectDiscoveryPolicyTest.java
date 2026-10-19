package com.histar.be.discovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.histar.be.discovery.service.DirectDiscoveryPolicy;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.entity.QuestStep;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.repository.QuestStepRepository;
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
class DirectDiscoveryPolicyTest {

    @Autowired private DirectDiscoveryPolicy policy;
    @Autowired private LocationRepository locationRepository;
    @Autowired private QuestRepository questRepository;
    @Autowired private QuestStepRepository questStepRepository;

    private UUID locationId;
    private UUID otherLocationId;

    @BeforeEach
    void setUp() {
        locationId = location("Bến Nhà Rồng test");
        otherLocationId = location("Nơi khác");
        quest("ONLINE", "artifact:online-view", "artifact");
        quest("ONSITE", "artifact:onsite-only", "artifact");
        quest("ONLINE", "artifact:minigame-reward", "minigame");
    }

    private UUID location(String name) {
        return locationRepository.save(Location.builder()
                        .name(name)
                        .latitude(10.0)
                        .longitude(106.0)
                        .city("TP.HCM")
                        .createdAt(Instant.now())
                        .build())
                .getId();
    }

    private void quest(String track, String key, String actionType) {
        Quest quest = questRepository.save(Quest.builder()
                .locationId(locationId)
                .title("Quest " + key)
                .pointsReward(10)
                .track(track)
                .build());
        questStepRepository.save(QuestStep.builder()
                .quest(quest)
                .stepOrder(1)
                .unlockKey(key)
                .title("Bước")
                .actionType(actionType)
                .build());
    }

    @Test
    void viewKeysAreAlwaysAllowed() {
        assertTrue(policy.allows(locationId, "scene:abc"));
        assertTrue(policy.allows(locationId, "era:1968"));
        assertTrue(policy.allows(locationId, "xuanay:bep-hoang-cam"));
    }

    @Test
    void challengeKeysNeedTheirOwnFlow() {
        assertFalse(policy.allows(locationId, "dialogue:anh-ba"));
        assertFalse(policy.allows(locationId, "minigame:duong-khoi"));
        assertFalse(policy.allows(locationId, "checkin:any"));
        assertFalse(policy.allows(locationId, " "));
    }

    @Test
    void artifactOnlyWhenItIsAnOnlineViewStepAtThatLocation() {
        assertTrue(policy.allows(locationId, "artifact:online-view"));
        assertFalse(policy.allows(otherLocationId, "artifact:online-view"));
        assertFalse(policy.allows(locationId, "artifact:onsite-only"));
        assertFalse(policy.allows(locationId, "artifact:minigame-reward"));
        assertFalse(policy.allows(locationId, "artifact:cuoc-chim"));
        assertFalse(policy.allows(null, "artifact:online-view"));
    }
}
